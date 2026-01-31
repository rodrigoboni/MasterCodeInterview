package howToSolveCodingProblems.transactionValidator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable transaction record with built-in validation.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERN: Immutable Object Pattern (using Java Record)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * WHY IMMUTABILITY MATTERS FOR CONCURRENCY?
 * ─────────────────────────────────────────
 * An immutable object's state cannot change after construction. This provides:
 *
 *   1. THREAD SAFETY BY DESIGN
 *      - No synchronization needed - multiple threads can read safely
 *      - No race conditions possible - nothing to race for!
 *      - No defensive copying needed when sharing between threads
 *
 *   2. FAILURE ATOMICITY
 *      - Object is either fully constructed or not at all
 *      - No partial/inconsistent states visible to other threads
 *
 *   3. SAFE PUBLICATION
 *      - Once constructed, can be freely shared without synchronization
 *      - All fields are effectively final (guaranteed by record)
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * JAVA FEATURE: Records (Java 16+)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Records are immutable data carriers that auto-generate:
 *   - Private final fields for each component
 *   - Public accessor methods (transactionId(), amount(), etc.)
 *   - Constructor with all parameters
 *   - equals() - component-by-component equality
 *   - hashCode() - based on all components
 *   - toString() - human-readable representation
 *
 * COMPACT CONSTRUCTOR:
 * ────────────────────
 * The constructor without parameters is called "compact constructor".
 * It runs BEFORE field assignment, allowing validation:
 *
 *   public Transaction {  // No parameters - it's compact!
 *       Objects.requireNonNull(transactionId);  // Validates
 *   }  // Fields are assigned AFTER this block
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * WHY BigDecimal FOR MONEY?
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * NEVER use float/double for money! Floating-point has precision issues:
 *
 *   double result = 0.1 + 0.2;
 *   System.out.println(result);  // Output: 0.30000000000000004  WRONG!
 *
 *   BigDecimal result = new BigDecimal("0.1").add(new BigDecimal("0.2"));
 *   System.out.println(result);  // Output: 0.3  CORRECT!
 *
 * BigDecimal provides:
 *   1. Arbitrary precision (no rounding errors)
 *   2. Exact decimal representation
 *   3. Control over rounding modes
 *   4. Immutable (thread-safe)
 *
 * INTERVIEW TIP:
 * ──────────────
 * Always mention BigDecimal when discussing financial systems.
 * It's a red flag if a candidate uses double for money.
 *
 * Time Complexity: O(1) for creation and validation
 * Space Complexity: O(1) per transaction
 */
public record Transaction(
        String transactionId,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        Instant timestamp
) {
    /**
     * Compact constructor - validates all fields.
     * Called automatically before field assignment.
     *
     * VALIDATION STRATEGY:
     * ────────────────────
     * 1. Null checks first (fail fast)
     * 2. Business rule checks second
     *
     * Why validate in constructor?
     * - Creates "always valid" objects
     * - No invalid Transaction can ever exist
     * - Simplifies rest of codebase (no null checks everywhere)
     */
    public Transaction {
        // Null checks - fail fast principle
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(type, "Transaction type cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        // Business rule validations
        if (transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be blank");
        }
        if (accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be blank");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
    }

    /**
     * Factory method with auto-generated timestamp.
     *
     * DESIGN PATTERN: Static Factory Method
     * ─────────────────────────────────────
     * Advantages over constructors:
     *   1. Has a descriptive name (create vs new Transaction)
     *   2. Can return cached instances (not applicable here, but useful for pools)
     *   3. Can return subtype (not applicable for records)
     *   4. Reduces parameter count at call site
     *
     * @param transactionId Unique identifier for this transaction
     * @param accountId The account this transaction affects
     * @param type The type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER)
     * @param amount The monetary amount (must be positive)
     * @return A new Transaction with current timestamp
     */
    public static Transaction create(String transactionId, String accountId,
                                     TransactionType type, BigDecimal amount) {
        return new Transaction(transactionId, accountId, type, amount, Instant.now());
    }

    /**
     * Convenience factory method accepting String amount.
     * Useful for creating transactions from user input.
     *
     * @param transactionId Unique identifier for this transaction
     * @param accountId The account this transaction affects
     * @param type The type of transaction
     * @param amount The monetary amount as a String (e.g., "100.50")
     * @return A new Transaction with current timestamp
     */
    public static Transaction create(String transactionId, String accountId,
                                     TransactionType type, String amount) {
        return create(transactionId, accountId, type, new BigDecimal(amount));
    }
}
