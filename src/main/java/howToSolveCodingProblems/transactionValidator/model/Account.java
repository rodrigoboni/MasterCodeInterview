package howToSolveCodingProblems.transactionValidator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe account with balance management and duplicate detection.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCURRENCY CONCEPTS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * This class demonstrates THREE different concurrency mechanisms:
 *
 *   ┌─────────────────┬────────────────────────┬──────────────────────────────┐
 *   │ Operation       │ Mechanism              │ Why This Choice?             │
 *   ├─────────────────┼────────────────────────┼──────────────────────────────┤
 *   │ Read balance    │ AtomicReference.get()  │ Single atomic read           │
 *   │ Deposit         │ CAS loop (optimistic)  │ No validation needed         │
 *   │ Withdrawal      │ ReentrantLock          │ Check-then-act atomicity     │
 *   │ Duplicate check │ ConcurrentHashMap      │ putIfAbsent is atomic        │
 *   └─────────────────┴────────────────────────┴──────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCEPT 1: AtomicReference for Balance
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * WHY AtomicReference<BigDecimal> INSTEAD OF AtomicLong?
 *
 *   Option A: AtomicLong (storing cents)
 *   ────────────────────────────────────
 *     AtomicLong balance = new AtomicLong(10000L);  // $100.00 as 10000 cents
 *
 *     PROS: Single atomic operations, no object allocation
 *     CONS: Limited to 2 decimal places, overflow risk, conversion code everywhere
 *
 *   Option B: AtomicReference<BigDecimal> (chosen)
 *   ───────────────────────────────────────────────
 *     AtomicReference<BigDecimal> balance = new AtomicReference<>(new BigDecimal("100.00"));
 *
 *     PROS: Arbitrary precision, natural representation, immutable BigDecimal
 *     CONS: Object allocation on each update
 *
 * CONCLUSION: For financial systems, precision trumps micro-optimization.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCEPT 2: ReentrantLock vs synchronized
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * The CHECK-THEN-ACT Problem:
 * ───────────────────────────
 *
 *   // BROKEN CODE - Race condition!
 *   if (balance.get().compareTo(amount) >= 0) {  // Thread A: $100 >= $80 ✓
 *       // Thread B also checks: $100 >= $50 ✓
 *       balance.updateAndGet(b -> b.subtract(amount));  // Thread A: $100-$80 = $20
 *       // Thread B: $20 - $50 = -$30  // NEGATIVE BALANCE!
 *   }
 *
 * Why ReentrantLock over synchronized?
 * ─────────────────────────────────────
 *   ┌──────────────────────┬────────────────────┬─────────────────────────┐
 *   │ Feature              │ synchronized       │ ReentrantLock           │
 *   ├──────────────────────┼────────────────────┼─────────────────────────┤
 *   │ Acquire with timeout │ ❌ No              │ ✓ tryLock(time, unit)   │
 *   │ Interruptible wait   │ ❌ No              │ ✓ lockInterruptibly()   │
 *   │ Fair ordering        │ ❌ No              │ ✓ new ReentrantLock(true│
 *   │ Multiple conditions  │ ❌ No              │ ✓ newCondition()        │
 *   │ Check if held        │ ❌ No              │ ✓ isLocked(), isHeldBy..│
 *   │ Automatic unlock     │ ✓ Yes             │ ❌ Manual in finally     │
 *   └──────────────────────┴────────────────────┴─────────────────────────┘
 *
 * We use ReentrantLock because:
 *   1. Fair lock (FIFO) prevents thread starvation
 *   2. Explicit control for debugging
 *   3. Industry standard for financial systems
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCEPT 3: ConcurrentHashMap for Duplicate Detection
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * THE KEY INSIGHT: putIfAbsent() is ATOMIC
 *
 *   // This is a single atomic operation:
 *   Instant previous = map.putIfAbsent(transactionId, Instant.now());
 *   boolean isNewTransaction = (previous == null);
 *
 * How it prevents duplicates:
 * ───────────────────────────
 *   Thread A: putIfAbsent("TXN-001", now) → returns null (added!)
 *   Thread B: putIfAbsent("TXN-001", now) → returns Instant (already exists!)
 *
 *   Even if both threads call simultaneously, only ONE succeeds in adding.
 *   This is guaranteed by ConcurrentHashMap's internal synchronization.
 *
 * Why store Instant instead of Boolean?
 * ─────────────────────────────────────
 *   1. Audit trail (when was it first processed?)
 *   2. Debugging information
 *   3. Potential for time-based cleanup of old entries
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCEPT 4: Compare-And-Swap (CAS) for Deposits
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * CAS is "optimistic locking" - assume no conflict, retry if wrong.
 *
 *   public void deposit(BigDecimal amount) {
 *       balance.updateAndGet(current -> current.add(amount));
 *   }
 *
 * Internally, updateAndGet does:
 *
 *   do {
 *       current = get();                        // 1. Read current value
 *       updated = function.apply(current);       // 2. Compute new value
 *   } while (!compareAndSet(current, updated)); // 3. Atomic swap if unchanged
 *
 * Example with contention:
 *
 *   T0: Thread A reads balance = $100
 *   T1: Thread A computes $100 + $50 = $150
 *   T1: Thread B reads balance = $100
 *   T2: Thread B computes $100 + $30 = $130
 *   T3: Thread A: CAS($100 → $150) → SUCCESS (balance now $150)
 *   T4: Thread B: CAS($100 → $130) → FAILS! (balance is $150, not $100)
 *   T5: Thread B RETRIES: reads $150, computes $180, CAS succeeds
 *
 *   Final: $180 ✓ (correct: $100 + $50 + $30)
 *
 * Time Complexity:
 *   - getBalance(): O(1)
 *   - deposit(): O(1) amortized (may retry under contention)
 *   - withdraw(): O(1) but blocks on lock contention
 *   - markProcessed(): O(1) average for ConcurrentHashMap
 *
 * Space Complexity: O(n) where n = number of processed transactions
 */
public class Account {

    private final String accountId;

    /**
     * AtomicReference for balance provides atomic read/write operations.
     *
     * Why not just a field with synchronized access?
     * - AtomicReference provides non-blocking reads (no lock acquisition)
     * - Supports CAS operations for lock-free updates
     * - Better scalability under high read contention
     */
    private final AtomicReference<BigDecimal> balance;

    /**
     * Tracks processed transaction IDs with their timestamps.
     *
     * ConcurrentHashMap provides:
     * - Thread-safe operations without external locking
     * - Fine-grained locking (segment-level, not entire map)
     * - Excellent scalability with multiple threads
     * - Atomic putIfAbsent() operation
     */
    private final ConcurrentHashMap<String, Instant> processedTransactions;

    /**
     * ReentrantLock for compound operations (check balance + withdraw).
     *
     * Fair lock (fair=true) ensures FIFO ordering:
     * - Threads acquire lock in the order they requested it
     * - Prevents thread starvation under high contention
     * - Slightly lower throughput than unfair lock
     * - Worth it for fairness in financial systems
     */
    private final ReentrantLock withdrawalLock;

    /**
     * Creates a new account with the specified initial balance.
     *
     * @param accountId Unique identifier for this account
     * @param initialBalance Starting balance (must be non-negative)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public Account(String accountId, BigDecimal initialBalance) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be null or negative");
        }

        this.accountId = accountId;
        this.balance = new AtomicReference<>(initialBalance);
        this.processedTransactions = new ConcurrentHashMap<>();
        // Fair=true ensures FIFO ordering to prevent starvation
        this.withdrawalLock = new ReentrantLock(true);
    }

    /**
     * @return The unique identifier for this account
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Get current balance (thread-safe read).
     *
     * This is a volatile read - guarantees visibility of the latest value
     * written by any thread. No locking required.
     *
     * @return Current account balance
     */
    public BigDecimal getBalance() {
        return balance.get();
    }

    /**
     * Deposit funds using CAS (Compare-And-Swap) loop.
     *
     * WHY CAS AND NOT LOCK?
     * ─────────────────────
     * Deposits always succeed (no balance check needed), so we use optimistic
     * locking. The CAS loop retries if another thread modified the balance
     * between our read and write.
     *
     * PERFORMANCE CHARACTERISTICS:
     * - Non-blocking: threads don't wait for each other
     * - Scalable: performance doesn't degrade much with more threads
     * - Fair: each thread eventually succeeds (lock-free, not wait-free)
     *
     * @param amount Amount to deposit (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // AtomicReference.updateAndGet handles the CAS loop internally
        // This is equivalent to:
        //   do {
        //       current = balance.get();
        //       updated = current.add(amount);
        //   } while (!balance.compareAndSet(current, updated));
        balance.updateAndGet(current -> current.add(amount));
    }

    /**
     * Withdraw funds with check-then-act atomicity.
     *
     * WHY LOCK AND NOT CAS?
     * ─────────────────────
     * Withdrawals require checking balance BEFORE modifying it.
     * With CAS alone, two concurrent withdrawals could both pass the check
     * and result in negative balance.
     *
     * THE RACE CONDITION WE'RE PREVENTING:
     *
     *   Time   Thread A                    Thread B
     *   ────   ────────────────────────    ────────────────────────
     *   T1     check: $100 >= $80 ✓
     *   T2                                 check: $100 >= $50 ✓
     *   T3     subtract: $100-$80 = $20
     *   T4                                 subtract: $20-$50 = -$30 ❌
     *
     * With ReentrantLock, Thread B must wait for Thread A to complete:
     *
     *   Time   Thread A                    Thread B
     *   ────   ────────────────────────    ────────────────────────
     *   T1     lock.lock()
     *   T2     check: $100 >= $80 ✓        lock.lock() [BLOCKED]
     *   T3     subtract: $100-$80 = $20    [waiting...]
     *   T4     lock.unlock()
     *   T5                                 [acquired lock]
     *   T6                                 check: $20 >= $50 ❌
     *   T7                                 return false
     *   T8                                 lock.unlock()
     *
     * @param amount Amount to withdraw (must be positive)
     * @return true if withdrawal succeeded, false if insufficient funds
     * @throws IllegalArgumentException if amount is not positive
     */
    public boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        withdrawalLock.lock();
        try {
            BigDecimal current = balance.get();
            if (current.compareTo(amount) < 0) {
                return false; // Insufficient funds
            }
            balance.set(current.subtract(amount));
            return true;
        } finally {
            // CRITICAL: Always unlock in finally block!
            // If we throw an exception after lock() but before unlock(),
            // the lock would be held forever (deadlock).
            withdrawalLock.unlock();
        }
    }

    /**
     * Check if a transaction has already been processed.
     *
     * Thread-safe read operation on ConcurrentHashMap.
     *
     * @param transactionId The transaction ID to check
     * @return true if already processed, false otherwise
     */
    public boolean isProcessed(String transactionId) {
        return processedTransactions.containsKey(transactionId);
    }

    /**
     * Mark a transaction as processed (atomic check-and-add).
     *
     * WHY putIfAbsent?
     * ────────────────
     * putIfAbsent is atomic - it checks AND adds in one operation.
     * This prevents race conditions where two threads both think
     * they're the first to process a transaction.
     *
     * EXAMPLE:
     *   Thread A: markProcessed("TXN-001") → returns true (added)
     *   Thread B: markProcessed("TXN-001") → returns false (already exists)
     *
     *   Even if both calls happen "simultaneously", only ONE returns true.
     *   This is guaranteed by ConcurrentHashMap's internal synchronization.
     *
     * @param transactionId The transaction ID to mark as processed
     * @return true if newly marked (this is the first time), false if already processed
     */
    public boolean markProcessed(String transactionId) {
        // putIfAbsent returns null if key was absent (newly added)
        // returns existing value if key was present
        return processedTransactions.putIfAbsent(transactionId, Instant.now()) == null;
    }

    /**
     * Get the timestamp when a transaction was first processed.
     *
     * Useful for:
     * - Audit trails
     * - Debugging duplicate submissions
     * - Determining transaction ordering
     *
     * @param transactionId The transaction ID to look up
     * @return Instant when first processed, or null if never processed
     */
    public Instant getProcessedTimestamp(String transactionId) {
        return processedTransactions.get(transactionId);
    }

    /**
     * Get count of processed transactions.
     *
     * Useful for metrics and monitoring.
     * Note: This is an O(n) operation for ConcurrentHashMap.size() in some cases.
     *
     * @return Number of unique transactions processed by this account
     */
    public int getProcessedCount() {
        return processedTransactions.size();
    }

    @Override
    public String toString() {
        return String.format("Account[id=%s, balance=%s, processedTransactions=%d]",
                accountId, balance.get(), processedTransactions.size());
    }
}
