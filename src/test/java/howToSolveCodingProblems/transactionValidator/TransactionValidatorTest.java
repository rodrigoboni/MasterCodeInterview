package howToSolveCodingProblems.transactionValidator;

import howToSolveCodingProblems.transactionValidator.model.Account;
import howToSolveCodingProblems.transactionValidator.model.Transaction;
import howToSolveCodingProblems.transactionValidator.model.TransactionType;
import howToSolveCodingProblems.transactionValidator.validation.DuplicateTransaction;
import howToSolveCodingProblems.transactionValidator.validation.InsufficientFunds;
import howToSolveCodingProblems.transactionValidator.validation.InvalidInput;
import howToSolveCodingProblems.transactionValidator.validation.Success;
import howToSolveCodingProblems.transactionValidator.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for TransactionValidator.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCURRENCY TESTING PATTERNS DEMONSTRATED
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Testing concurrent code is challenging because:
 *   1. Race conditions are non-deterministic
 *   2. Bugs may only appear under specific timing
 *   3. Thread scheduling varies between runs
 *
 * PATTERNS USED IN THIS TEST SUITE:
 *
 *   1. CountDownLatch (Starting Gun Pattern)
 *      ─────────────────────────────────────
 *      Ensures all threads start simultaneously to maximize contention.
 *
 *      CountDownLatch startLatch = new CountDownLatch(1);
 *      // Submit tasks that call startLatch.await()
 *      startLatch.countDown();  // Release all threads at once!
 *
 *   2. ExecutorService + Fixed Thread Pool
 *      ────────────────────────────────────
 *      Manages thread lifecycle and provides controlled concurrency.
 *
 *      ExecutorService executor = Executors.newFixedThreadPool(100);
 *      // Submit tasks
 *      executor.shutdown();
 *      executor.awaitTermination(timeout, unit);
 *
 *   3. AtomicInteger for Thread-Safe Counting
 *      ──────────────────────────────────────
 *      Safely counts results from multiple threads.
 *
 *      AtomicInteger successCount = new AtomicInteger(0);
 *      successCount.incrementAndGet();  // Thread-safe increment
 *
 *   4. Deterministic Assertions
 *      ────────────────────────
 *      For well-designed concurrent code, outcomes should be deterministic
 *      even if the order of operations is not.
 *
 *      assertEquals(20, successCount.get());  // Exactly 20 withdrawals succeed
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * TEST ORGANIZATION (Following project conventions)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *   @Nested classes group related tests:
 *     - BasicValidationTests: Core functionality
 *     - DuplicateDetectionTests: Idempotency
 *     - ConcurrencyTests: Thread safety
 *     - EdgeCaseTests: Boundary conditions
 *     - PatternMatchingTests: Java 17+ features
 */
@DisplayName("TransactionValidator")
class TransactionValidatorTest {

    private TransactionValidator validator;
    private static final String ACCOUNT_ID = "ACC-001";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
        validator.createAccount(ACCOUNT_ID, INITIAL_BALANCE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BASIC FUNCTIONALITY TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Basic Validation Tests")
    class BasicValidationTests {

        @Test
        @DisplayName("accepts valid deposit")
        void validDeposit() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("100.00"));

            ValidationResult result = validator.process(tx);

            assertInstanceOf(Success.class, result);
            Success success = (Success) result;
            assertEquals(new BigDecimal("1100.00"), success.newBalance());
        }

        @Test
        @DisplayName("accepts valid withdrawal when sufficient funds")
        void validWithdrawal() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, new BigDecimal("100.00"));

            ValidationResult result = validator.process(tx);

            assertInstanceOf(Success.class, result);
            assertEquals(new BigDecimal("900.00"),
                    validator.getAccount(ACCOUNT_ID).get().getBalance());
        }

        @Test
        @DisplayName("rejects withdrawal when insufficient funds")
        void insufficientFunds() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, new BigDecimal("2000.00"));

            ValidationResult result = validator.process(tx);

            assertInstanceOf(InsufficientFunds.class, result);
            InsufficientFunds failure = (InsufficientFunds) result;
            assertEquals(INITIAL_BALANCE, failure.currentBalance());
            assertEquals(new BigDecimal("2000.00"), failure.requestedAmount());
            assertEquals(new BigDecimal("1000.00"), failure.shortfall());
        }

        @Test
        @DisplayName("rejects null transaction")
        void nullTransaction() {
            ValidationResult result = validator.process(null);

            assertInstanceOf(InvalidInput.class, result);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("rejects unknown account")
        void unknownAccount() {
            Transaction tx = Transaction.create("TXN-001", "UNKNOWN-ACCOUNT",
                    TransactionType.DEPOSIT, new BigDecimal("100.00"));

            ValidationResult result = validator.process(tx);

            assertInstanceOf(InvalidInput.class, result);
            assertTrue(result.message().contains("not found"));
        }

        @Test
        @DisplayName("processes transfer as withdrawal from source account")
        void transferAsWithdrawal() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.TRANSFER, new BigDecimal("300.00"));

            ValidationResult result = validator.process(tx);

            assertInstanceOf(Success.class, result);
            assertEquals(new BigDecimal("700.00"),
                    validator.getAccount(ACCOUNT_ID).get().getBalance());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DUPLICATE DETECTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Duplicate Detection Tests")
    class DuplicateDetectionTests {

        @Test
        @DisplayName("rejects duplicate transaction ID")
        void rejectsDuplicate() {
            Transaction tx1 = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("100.00"));
            Transaction tx2 = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("200.00"));

            ValidationResult result1 = validator.process(tx1);
            ValidationResult result2 = validator.process(tx2);

            assertInstanceOf(Success.class, result1);
            assertInstanceOf(DuplicateTransaction.class, result2);
            // Balance should only reflect the first deposit
            assertEquals(new BigDecimal("1100.00"),
                    validator.getAccount(ACCOUNT_ID).get().getBalance());
        }

        @Test
        @DisplayName("accepts same amount with different transaction ID")
        void acceptsDifferentId() {
            Transaction tx1 = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("100.00"));
            Transaction tx2 = Transaction.create("TXN-002", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("100.00"));

            ValidationResult r1 = validator.process(tx1);
            ValidationResult r2 = validator.process(tx2);

            assertTrue(r1.isSuccess());
            assertTrue(r2.isSuccess());
            // Both deposits should be applied
            assertEquals(new BigDecimal("1200.00"),
                    validator.getAccount(ACCOUNT_ID).get().getBalance());
        }

        @Test
        @DisplayName("marks failed transactions to prevent replay attacks")
        void failedTransactionStillMarked() {
            // First attempt: insufficient funds
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, new BigDecimal("5000.00"));
            ValidationResult result1 = validator.process(tx);

            // Second attempt with same ID: should be duplicate
            Transaction retry = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, new BigDecimal("100.00"));
            ValidationResult result2 = validator.process(retry);

            assertInstanceOf(InsufficientFunds.class, result1);
            assertInstanceOf(DuplicateTransaction.class, result2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONCURRENCY TESTS - THE CRITICAL INTERVIEW MATERIAL
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        /**
         * TEST: Concurrent Withdrawals Should Not Exceed Balance
         *
         * SCENARIO:
         * ─────────
         * - Account has $1000
         * - 100 threads each try to withdraw $50
         * - Without proper locking, all 100 might pass balance check
         * - Expected: Only 20 should succeed ($1000 / $50 = 20)
         *
         * TESTING PATTERN: CountDownLatch (Starting Gun)
         * ───────────────────────────────────────────────
         * All threads wait at startLatch.await(), then start simultaneously
         * when we call startLatch.countDown(). This maximizes contention.
         */
        @Test
        @DisplayName("concurrent withdrawals do not exceed balance")
        void concurrentWithdrawalsDoNotExceedBalance() throws InterruptedException {
            // Setup: Create fresh account with exactly $1000
            TransactionValidator localValidator = new TransactionValidator();
            localValidator.createAccount("CONC-001", new BigDecimal("1000.00"));

            int numThreads = 100;
            BigDecimal withdrawalAmount = new BigDecimal("50.00");
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);  // Starting gun
            CountDownLatch doneLatch = new CountDownLatch(numThreads);  // Finish line

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Submit all withdrawal tasks
            for (int i = 0; i < numThreads; i++) {
                final int txNum = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();  // Wait for starting gun
                        Transaction tx = Transaction.create("TXN-" + txNum, "CONC-001",
                                TransactionType.WITHDRAWAL, withdrawalAmount);

                        ValidationResult result = localValidator.process(tx);

                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Fire the starting gun!
            startLatch.countDown();

            // Wait for all threads to complete
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
                    "Threads should complete within timeout");
            executor.shutdown();

            // Verify: Only 20 withdrawals of $50 should succeed from $1000
            assertEquals(20, successCount.get(),
                    "Exactly 20 withdrawals of $50 should succeed from $1000 balance");
            assertEquals(80, failureCount.get(),
                    "Exactly 80 withdrawals should fail due to insufficient funds");

            // Verify final balance is $0
            BigDecimal finalBalance = localValidator.getAccount("CONC-001").get().getBalance();
            assertEquals(0, BigDecimal.ZERO.compareTo(finalBalance),
                    "Final balance should be exactly $0");
        }

        /**
         * TEST: Concurrent Duplicate Detection
         *
         * SCENARIO:
         * ─────────
         * - 100 threads submit the SAME transaction ID simultaneously
         * - Only ONE should succeed; 99 should get DuplicateTransaction
         *
         * This tests the atomicity of ConcurrentHashMap.putIfAbsent()
         */
        @Test
        @DisplayName("concurrent duplicate submissions - only one succeeds")
        void concurrentDuplicateSubmissions() throws InterruptedException {
            int numThreads = 100;
            String duplicateId = "DUPLICATE-TXN-001";
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger duplicateCount = new AtomicInteger(0);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Transaction tx = Transaction.create(duplicateId, ACCOUNT_ID,
                                TransactionType.DEPOSIT, new BigDecimal("10.00"));

                        ValidationResult result = validator.process(tx);

                        if (result instanceof Success) {
                            successCount.incrementAndGet();
                        } else if (result instanceof DuplicateTransaction) {
                            duplicateCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(1, successCount.get(),
                    "Exactly one transaction should succeed");
            assertEquals(99, duplicateCount.get(),
                    "All other transactions should be detected as duplicates");
        }

        /**
         * TEST: Concurrent Deposits Accumulate Correctly
         *
         * SCENARIO:
         * ─────────
         * - 1000 threads each deposit $1.00
         * - Starting balance: $0
         * - Expected final balance: $1000
         *
         * This tests the CAS loop in deposit() - no updates should be lost.
         */
        @Test
        @DisplayName("concurrent deposits accumulate correctly")
        void concurrentDepositsAccumulateCorrectly() throws InterruptedException {
            TransactionValidator localValidator = new TransactionValidator();
            localValidator.createAccount("DEP-001", BigDecimal.ZERO);

            int numDeposits = 1000;
            BigDecimal depositAmount = new BigDecimal("1.00");
            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numDeposits);

            for (int i = 0; i < numDeposits; i++) {
                final int txNum = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Transaction tx = Transaction.create("DEP-" + txNum, "DEP-001",
                                TransactionType.DEPOSIT, depositAmount);
                        localValidator.process(tx);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            BigDecimal expectedBalance = new BigDecimal("1000.00");
            BigDecimal actualBalance = localValidator.getAccount("DEP-001").get().getBalance();

            assertEquals(0, expectedBalance.compareTo(actualBalance),
                    "All deposits should be applied. Expected: " + expectedBalance +
                            ", Actual: " + actualBalance);
        }

        /**
         * TEST: Mixed Concurrent Operations Maintain Consistency
         *
         * SCENARIO:
         * ─────────
         * - Start with $500
         * - 50 threads deposit $20 each = +$1000
         * - 50 threads withdraw $10 each (some may fail due to timing)
         * - Verify final balance is consistent and non-negative
         *
         * NOTE ON CONCURRENCY TESTING:
         * ────────────────────────────
         * Mixed concurrent operations are inherently non-deterministic.
         * The order of deposit vs withdrawal execution affects which
         * withdrawals succeed. We can verify:
         *   1. All deposits succeed (they always should)
         *   2. Balance is never negative
         *   3. Final balance equals: initial + deposits - successful withdrawals
         */
        @Test
        @DisplayName("mixed concurrent deposits and withdrawals maintain consistency")
        void mixedConcurrentOperations() throws InterruptedException {
            TransactionValidator localValidator = new TransactionValidator();
            BigDecimal initialBalance = new BigDecimal("500.00");
            localValidator.createAccount("MIXED-001", initialBalance);

            ExecutorService executor = Executors.newFixedThreadPool(100);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(100);

            AtomicInteger depositSuccessCount = new AtomicInteger(0);
            AtomicInteger withdrawSuccessCount = new AtomicInteger(0);

            BigDecimal depositAmount = new BigDecimal("20.00");
            BigDecimal withdrawAmount = new BigDecimal("10.00");

            // Submit 50 deposits of $20 each
            for (int i = 0; i < 50; i++) {
                final int txNum = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Transaction tx = Transaction.create("DEP-" + txNum, "MIXED-001",
                                TransactionType.DEPOSIT, depositAmount);
                        if (localValidator.process(tx).isSuccess()) {
                            depositSuccessCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Submit 50 withdrawals of $10 each
            for (int i = 0; i < 50; i++) {
                final int txNum = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Transaction tx = Transaction.create("WTH-" + txNum, "MIXED-001",
                                TransactionType.WITHDRAWAL, withdrawAmount);
                        if (localValidator.process(tx).isSuccess()) {
                            withdrawSuccessCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            BigDecimal actualBalance = localValidator.getAccount("MIXED-001").get().getBalance();

            // Verify all deposits succeeded (deposits always succeed)
            assertEquals(50, depositSuccessCount.get(),
                    "All 50 deposits should succeed");

            // Verify balance is non-negative (critical invariant)
            assertTrue(actualBalance.compareTo(BigDecimal.ZERO) >= 0,
                    "Balance should never be negative, but was: " + actualBalance);

            // Verify balance is mathematically consistent with operations
            BigDecimal totalDeposited = depositAmount.multiply(new BigDecimal(depositSuccessCount.get()));
            BigDecimal totalWithdrawn = withdrawAmount.multiply(new BigDecimal(withdrawSuccessCount.get()));
            BigDecimal expectedBalance = initialBalance.add(totalDeposited).subtract(totalWithdrawn);

            assertEquals(0, expectedBalance.compareTo(actualBalance),
                    String.format("Balance should be consistent. " +
                                    "Initial: %s, Deposited: %s (×%d), Withdrawn: %s (×%d), " +
                                    "Expected: %s, Actual: %s",
                            initialBalance, depositAmount, depositSuccessCount.get(),
                            withdrawAmount, withdrawSuccessCount.get(),
                            expectedBalance, actualBalance));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EDGE CASE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("rejects zero amount transaction")
        void rejectsZeroAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    Transaction.create("TXN-001", ACCOUNT_ID,
                            TransactionType.WITHDRAWAL, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("rejects negative amount transaction")
        void rejectsNegativeAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    Transaction.create("TXN-001", ACCOUNT_ID,
                            TransactionType.WITHDRAWAL, new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("handles exact balance withdrawal")
        void exactBalanceWithdrawal() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, INITIAL_BALANCE);

            ValidationResult result = validator.process(tx);

            assertTrue(result.isSuccess());
            assertEquals(0, BigDecimal.ZERO.compareTo(
                    validator.getAccount(ACCOUNT_ID).get().getBalance()));
        }

        @Test
        @DisplayName("handles very small amounts (cents)")
        void verySmallAmounts() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("0.01"));

            ValidationResult result = validator.process(tx);

            assertTrue(result.isSuccess());
            assertEquals(new BigDecimal("1000.01"),
                    validator.getAccount(ACCOUNT_ID).get().getBalance());
        }

        @Test
        @DisplayName("handles very large amounts")
        void veryLargeAmounts() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.DEPOSIT, new BigDecimal("999999999999.99"));

            ValidationResult result = validator.process(tx);

            assertTrue(result.isSuccess());
        }

        @ParameterizedTest(name = "withdrawing ${0} from ${1} balance should {2}")
        @CsvSource({
                "100.00, 1000.00, succeed",
                "1000.00, 1000.00, succeed",
                "1000.01, 1000.00, fail"
        })
        @DisplayName("boundary withdrawal amounts")
        void boundaryWithdrawals(String withdrawal, String balance, String expected) {
            TransactionValidator localValidator = new TransactionValidator();
            localValidator.createAccount("BOUNDARY", new BigDecimal(balance));

            Transaction tx = Transaction.create("TXN-001", "BOUNDARY",
                    TransactionType.WITHDRAWAL, new BigDecimal(withdrawal));

            ValidationResult result = localValidator.process(tx);

            if (expected.equals("succeed")) {
                assertTrue(result.isSuccess(), "Expected success for withdrawal of $" + withdrawal);
            } else {
                assertFalse(result.isSuccess(), "Expected failure for withdrawal of $" + withdrawal);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("rejects blank transaction IDs")
        void rejectsBlankTransactionId(String blankId) {
            assertThrows(IllegalArgumentException.class, () ->
                    Transaction.create(blankId, ACCOUNT_ID,
                            TransactionType.DEPOSIT, new BigDecimal("100.00")));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERN MATCHING TESTS (Java 17+)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Pattern Matching Tests")
    class PatternMatchingTests {

        @Test
        @DisplayName("demonstrates exhaustive pattern matching on ValidationResult")
        void patternMatchingDemo() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, new BigDecimal("100.00"));

            ValidationResult result = validator.process(tx);

            // Java 17 pattern matching with instanceof
            // In Java 21+, you could use switch pattern matching for cleaner syntax
            String message;
            if (result instanceof Success s) {
                message = "Success! New balance: " + s.newBalance();
            } else if (result instanceof InsufficientFunds f) {
                message = "Failed: Need " + f.requestedAmount() +
                        " but only have " + f.currentBalance();
            } else if (result instanceof DuplicateTransaction d) {
                message = "Duplicate: " + d.transactionId();
            } else if (result instanceof InvalidInput i) {
                message = "Invalid: " + i.reason();
            } else {
                message = "Unknown result type";
            }

            assertNotNull(message);
            assertTrue(message.startsWith("Success"));
        }

        @Test
        @DisplayName("pattern matching extracts correct data from InsufficientFunds")
        void patternMatchingInsufficientFunds() {
            Transaction tx = Transaction.create("TXN-001", ACCOUNT_ID,
                    TransactionType.WITHDRAWAL, new BigDecimal("2000.00"));

            ValidationResult result = validator.process(tx);

            // Pattern matching with instanceof
            if (result instanceof InsufficientFunds f) {
                assertEquals(INITIAL_BALANCE, f.currentBalance());
                assertEquals(new BigDecimal("2000.00"), f.requestedAmount());
                assertEquals(new BigDecimal("1000.00"), f.shortfall());
            } else {
                fail("Expected InsufficientFunds but got " + result.getClass().getSimpleName());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCOUNT CREATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Account Creation Tests")
    class AccountCreationTests {

        @Test
        @DisplayName("creates account with valid parameters")
        void createsValidAccount() {
            TransactionValidator localValidator = new TransactionValidator();
            Account account = localValidator.createAccount("NEW-001", new BigDecimal("500.00"));

            assertNotNull(account);
            assertEquals("NEW-001", account.getAccountId());
            assertEquals(new BigDecimal("500.00"), account.getBalance());
        }

        @Test
        @DisplayName("rejects duplicate account creation")
        void rejectsDuplicateAccount() {
            TransactionValidator localValidator = new TransactionValidator();
            localValidator.createAccount("DUP-001", BigDecimal.ZERO);

            assertThrows(IllegalArgumentException.class, () ->
                    localValidator.createAccount("DUP-001", BigDecimal.ZERO));
        }

        @Test
        @DisplayName("rejects negative initial balance")
        void rejectsNegativeBalance() {
            TransactionValidator localValidator = new TransactionValidator();

            assertThrows(IllegalArgumentException.class, () ->
                    localValidator.createAccount("NEG-001", new BigDecimal("-100.00")));
        }

        @Test
        @DisplayName("allows zero initial balance")
        void allowsZeroBalance() {
            TransactionValidator localValidator = new TransactionValidator();
            Account account = localValidator.createAccount("ZERO-001", BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        }
    }
}
