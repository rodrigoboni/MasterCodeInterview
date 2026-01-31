package howToSolveCodingProblems.transactionValidator;

import howToSolveCodingProblems.transactionValidator.model.Account;
import howToSolveCodingProblems.transactionValidator.model.Transaction;
import howToSolveCodingProblems.transactionValidator.model.TransactionType;
import howToSolveCodingProblems.transactionValidator.validation.DuplicateTransaction;
import howToSolveCodingProblems.transactionValidator.validation.InsufficientFunds;
import howToSolveCodingProblems.transactionValidator.validation.InvalidInput;
import howToSolveCodingProblems.transactionValidator.validation.Success;
import howToSolveCodingProblems.transactionValidator.validation.ValidationResult;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe transaction validator and processor service.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERN: Service Layer Pattern
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * This class acts as a Service that orchestrates:
 *   - Account management
 *   - Transaction validation
 *   - Transaction execution
 *
 * SERVICE LAYER RESPONSIBILITIES:
 *   1. Coordinate business operations
 *   2. Ensure transactional integrity
 *   3. Hide complexity from clients
 *   4. Provide a clean API
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * VALIDATION FLOW
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *   Transaction
 *       │
 *       ▼
 *   ┌─────────────────┐
 *   │ 1. Null check   │──── null? ───────────▶ InvalidInput
 *   └────────┬────────┘
 *            ▼
 *   ┌─────────────────┐
 *   │ 2. Find Account │──── not found? ──────▶ InvalidInput
 *   └────────┬────────┘
 *            ▼
 *   ┌─────────────────┐
 *   │ 3. Check Dupe   │──── duplicate? ──────▶ DuplicateTransaction
 *   └────────┬────────┘                        (using putIfAbsent)
 *            ▼
 *   ┌─────────────────┐
 *   │ 4. Execute Txn  │──── insufficient? ───▶ InsufficientFunds
 *   └────────┬────────┘     (WITHDRAWAL only)
 *            ▼
 *        Success
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CRITICAL CONCURRENCY INSIGHT: Order of Operations
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * The order of validation steps is CRITICAL for correctness:
 *
 *   Step 3 (Duplicate check) MUST happen BEFORE Step 4 (Balance check/Execute)
 *
 * WHY? Consider this scenario with reversed order:
 *
 *   1. Thread A: Balance check passes ($100 >= $80)
 *   2. Thread B: Balance check passes ($100 >= $80) // Same transaction ID!
 *   3. Thread A: Withdraws $80, balance = $20
 *   4. Thread B: Withdraws $80, balance = -$60  // NEGATIVE!
 *   5. Thread A: Marks "TXN-001" as processed
 *   6. Thread B: Marks "TXN-001" as processed (too late!)
 *
 * With correct order:
 *
 *   1. Thread A: markProcessed("TXN-001") → true (first!)
 *   2. Thread B: markProcessed("TXN-001") → false (blocked!)
 *   3. Thread A: Withdraws $80, balance = $20
 *   4. Thread B: Returns DuplicateTransaction
 *
 * DESIGN PATTERN: Fail-Fast
 * ─────────────────────────
 * We check duplicates BEFORE doing expensive operations.
 * This prevents:
 *   1. Wasted computation
 *   2. Race conditions
 *   3. Replay attacks
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * COMPLEXITY ANALYSIS
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *   Operation              │ Time           │ Space
 *   ───────────────────────┼────────────────┼─────────────────
 *   createAccount()        │ O(1)           │ O(1)
 *   getAccount()           │ O(1)           │ O(1)
 *   validate()             │ O(1)           │ O(1)
 *   process()              │ O(1)           │ O(1)
 *   ───────────────────────┼────────────────┼─────────────────
 *   Total for n accounts,  │ O(1) per op    │ O(a + t)
 *   t transactions         │                │ a=accounts, t=txns
 */
public class TransactionValidator {

    /**
     * Thread-safe map of account ID to Account.
     *
     * ConcurrentHashMap allows concurrent reads and writes without external locking.
     * This is safe because:
     *   1. Each Account handles its own synchronization internally
     *   2. putIfAbsent() ensures atomic account creation
     *   3. get() is a non-blocking read
     */
    private final ConcurrentHashMap<String, Account> accounts;

    /**
     * Creates a new TransactionValidator with no accounts.
     */
    public TransactionValidator() {
        this.accounts = new ConcurrentHashMap<>();
    }

    /**
     * Create a new account.
     *
     * Uses putIfAbsent for atomic creation - no external locking needed.
     *
     * @param accountId      Unique account identifier
     * @param initialBalance Starting balance (must be non-negative)
     * @return The created account
     * @throws IllegalArgumentException if account already exists or parameters invalid
     */
    public Account createAccount(String accountId, BigDecimal initialBalance) {
        Account newAccount = new Account(accountId, initialBalance);
        Account existing = accounts.putIfAbsent(accountId, newAccount);
        if (existing != null) {
            throw new IllegalArgumentException("Account already exists: " + accountId);
        }
        return newAccount;
    }

    /**
     * Get an account by ID.
     *
     * Returns Optional to force caller to handle "not found" case explicitly.
     *
     * DESIGN PATTERN: Optional instead of null
     * ─────────────────────────────────────────
     * Optional makes the possibility of absence explicit in the type system.
     * Caller must use .orElse(), .orElseThrow(), .ifPresent(), etc.
     *
     * @param accountId The account ID to look up
     * @return Optional containing the account, or empty if not found
     */
    public Optional<Account> getAccount(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    /**
     * Validate a transaction WITHOUT executing it.
     *
     * Useful for pre-validation in UI or API layers to give early feedback.
     * Does NOT mark the transaction as processed (idempotent operation).
     *
     * @param transaction The transaction to validate
     * @return ValidationResult indicating success or specific failure reason
     */
    public ValidationResult validate(Transaction transaction) {
        // Step 1: Null check
        if (transaction == null) {
            return new InvalidInput("Transaction cannot be null");
        }

        // Step 2: Find account
        Account account = accounts.get(transaction.accountId());
        if (account == null) {
            return new InvalidInput("Account not found: " + transaction.accountId());
        }

        // Step 3: Check for duplicate (read-only check)
        if (account.isProcessed(transaction.transactionId())) {
            return new DuplicateTransaction(transaction.transactionId());
        }

        // Step 4: Check balance for withdrawals
        if (transaction.type() == TransactionType.WITHDRAWAL ||
            transaction.type() == TransactionType.TRANSFER) {
            BigDecimal currentBalance = account.getBalance();
            if (currentBalance.compareTo(transaction.amount()) < 0) {
                return new InsufficientFunds(currentBalance, transaction.amount());
            }
        }

        // All validations passed
        return new Success(account.getBalance());
    }

    /**
     * Validate AND process a transaction atomically.
     *
     * This is the main entry point for processing transactions.
     * It ensures:
     *   1. Duplicate detection (atomic via putIfAbsent)
     *   2. Balance validation (atomic via ReentrantLock in Account)
     *   3. Balance update (atomic via ReentrantLock in Account)
     *
     * IMPORTANT: Once markProcessed() returns true, the transaction ID is
     * permanently recorded, even if the subsequent operation fails.
     * This prevents replay attacks with the same ID.
     *
     * @param transaction The transaction to process
     * @return ValidationResult indicating success or specific failure reason
     */
    public ValidationResult process(Transaction transaction) {
        // Step 1: Basic validation
        if (transaction == null) {
            return new InvalidInput("Transaction cannot be null");
        }

        // Step 2: Find account
        Account account = accounts.get(transaction.accountId());
        if (account == null) {
            return new InvalidInput("Account not found: " + transaction.accountId());
        }

        // Step 3: Atomic duplicate check-and-mark
        // ═══════════════════════════════════════════════════════════════════
        // CRITICAL: This MUST happen BEFORE balance check!
        // ═══════════════════════════════════════════════════════════════════
        // markProcessed() uses ConcurrentHashMap.putIfAbsent() internally,
        // which is an atomic operation. Only ONE thread can successfully
        // mark a given transaction ID.
        if (!account.markProcessed(transaction.transactionId())) {
            return new DuplicateTransaction(transaction.transactionId());
        }

        // Step 4: Execute based on transaction type
        // At this point, we've "claimed" this transaction ID.
        // Even if the operation fails, the ID remains marked to prevent replays.
        //
        // Note: Using traditional switch statement for Java 17 compatibility.
        // In Java 21+, you could use switch expressions with pattern matching.
        switch (transaction.type()) {
            case WITHDRAWAL:
            case TRANSFER:
                // withdraw() is internally synchronized with ReentrantLock
                // It atomically checks balance AND updates it
                if (!account.withdraw(transaction.amount())) {
                    return new InsufficientFunds(account.getBalance(), transaction.amount());
                }
                return new Success(account.getBalance());

            case DEPOSIT:
                // deposit() uses CAS loop - always succeeds for valid amounts
                account.deposit(transaction.amount());
                return new Success(account.getBalance());

            default:
                return new InvalidInput("Unknown transaction type: " + transaction.type());
        }
    }

    /**
     * Get the total number of accounts.
     *
     * @return Number of accounts managed by this validator
     */
    public int getAccountCount() {
        return accounts.size();
    }

    @Override
    public String toString() {
        return String.format("TransactionValidator[accounts=%d]", accounts.size());
    }
}
