package howToSolveCodingProblems.transactionValidator;

import howToSolveCodingProblems.transactionValidator.model.Transaction;
import howToSolveCodingProblems.transactionValidator.model.TransactionType;
import howToSolveCodingProblems.transactionValidator.validation.DuplicateTransaction;
import howToSolveCodingProblems.transactionValidator.validation.InsufficientFunds;
import howToSolveCodingProblems.transactionValidator.validation.InvalidInput;
import howToSolveCodingProblems.transactionValidator.validation.Success;
import howToSolveCodingProblems.transactionValidator.validation.ValidationResult;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstration of the TransactionValidator system.
 *
 * This demo showcases:
 *   1. Basic transaction processing (deposits, withdrawals)
 *   2. Duplicate transaction detection
 *   3. Insufficient funds handling
 *   4. Concurrent transaction processing
 *   5. Pattern matching on ValidationResult
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * RUNNING THIS DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *   From command line:
 *     cd MasterCodeInterview
 *     mvn compile exec:java -Dexec.mainClass="howToSolveCodingProblems.transactionValidator.TransactionValidatorDemo"
 *
 *   Or simply run the main() method from your IDE.
 */
public class TransactionValidatorDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("═".repeat(70));
        System.out.println("      TRANSACTION VALIDATOR DEMO");
        System.out.println("═".repeat(70));

        TransactionValidator validator = new TransactionValidator();

        // Create test account with $1000
        validator.createAccount("ACC-001", new BigDecimal("1000.00"));
        System.out.println("\n✓ Created account ACC-001 with $1000.00 balance");

        // ─────────────────────────────────────────────────────────────────────
        // Demo 1: Basic Transactions
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n" + "─".repeat(70));
        System.out.println("DEMO 1: Basic Transactions");
        System.out.println("─".repeat(70));

        // Successful deposit
        Transaction deposit = Transaction.create("TXN-001", "ACC-001",
                TransactionType.DEPOSIT, new BigDecimal("500.00"));
        ValidationResult depositResult = validator.process(deposit);
        printResult("Deposit $500", depositResult);

        // Successful withdrawal
        Transaction withdrawal = Transaction.create("TXN-002", "ACC-001",
                TransactionType.WITHDRAWAL, new BigDecimal("200.00"));
        ValidationResult withdrawalResult = validator.process(withdrawal);
        printResult("Withdraw $200", withdrawalResult);

        // ─────────────────────────────────────────────────────────────────────
        // Demo 2: Duplicate Detection
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n" + "─".repeat(70));
        System.out.println("DEMO 2: Duplicate Transaction Detection");
        System.out.println("─".repeat(70));

        // Try to process the same transaction ID again
        Transaction duplicateDeposit = Transaction.create("TXN-001", "ACC-001",
                TransactionType.DEPOSIT, new BigDecimal("100.00"));
        ValidationResult duplicateResult = validator.process(duplicateDeposit);
        printResult("Duplicate TXN-001", duplicateResult);

        // ─────────────────────────────────────────────────────────────────────
        // Demo 3: Insufficient Funds
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n" + "─".repeat(70));
        System.out.println("DEMO 3: Insufficient Funds Handling");
        System.out.println("─".repeat(70));

        // Current balance is $1300 ($1000 + $500 - $200)
        Transaction bigWithdrawal = Transaction.create("TXN-003", "ACC-001",
                TransactionType.WITHDRAWAL, new BigDecimal("5000.00"));
        ValidationResult insufficientResult = validator.process(bigWithdrawal);
        printResult("Withdraw $5000", insufficientResult);

        // ─────────────────────────────────────────────────────────────────────
        // Demo 4: Pattern Matching (Java 17+)
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n" + "─".repeat(70));
        System.out.println("DEMO 4: Pattern Matching on Results");
        System.out.println("─".repeat(70));

        Transaction testTxn = Transaction.create("TXN-004", "ACC-001",
                TransactionType.WITHDRAWAL, new BigDecimal("100.00"));
        ValidationResult result = validator.process(testTxn);

        // Pattern matching with instanceof (Java 17 standard feature)
        // In Java 21+, you could use switch pattern matching instead
        String response;
        if (result instanceof Success s) {
            response = String.format("✓ SUCCESS: New balance is $%s", s.newBalance());
        } else if (result instanceof InsufficientFunds f) {
            response = String.format("✗ DECLINED: Need $%s more", f.shortfall());
        } else if (result instanceof DuplicateTransaction d) {
            response = String.format("✗ DUPLICATE: %s already processed", d.transactionId());
        } else if (result instanceof InvalidInput i) {
            response = String.format("✗ INVALID: %s", i.reason());
        } else {
            response = "Unknown result type";
        }
        System.out.println("Pattern matching result: " + response);

        // ─────────────────────────────────────────────────────────────────────
        // Demo 5: Concurrent Transaction Processing
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n" + "─".repeat(70));
        System.out.println("DEMO 5: Concurrent Transaction Processing");
        System.out.println("─".repeat(70));

        demoConcurrentTransactions(validator);

        // ─────────────────────────────────────────────────────────────────────
        // Demo 6: Concurrent Duplicate Detection
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n" + "─".repeat(70));
        System.out.println("DEMO 6: Concurrent Duplicate Detection");
        System.out.println("─".repeat(70));

        demoConcurrentDuplicateDetection(validator);

        // Final balance
        System.out.println("\n" + "═".repeat(70));
        BigDecimal finalBalance = validator.getAccount("ACC-001")
                .map(a -> a.getBalance())
                .orElse(BigDecimal.ZERO);
        System.out.println("FINAL BALANCE: $" + finalBalance);
        System.out.println("═".repeat(70));
    }

    /**
     * Demonstrates concurrent withdrawals don't exceed balance.
     *
     * CONCURRENCY TESTING PATTERN: CountDownLatch
     * ────────────────────────────────────────────
     * CountDownLatch acts as a "starting gun" - all threads wait at the latch,
     * then start simultaneously when countdown reaches zero.
     *
     * This maximizes the chance of race conditions being exposed by ensuring
     * threads compete for the same resources at the exact same moment.
     */
    private static void demoConcurrentTransactions(TransactionValidator validator)
            throws InterruptedException {

        // Create a fresh account for this demo
        validator.createAccount("CONCURRENT-001", new BigDecimal("1000.00"));

        int numThreads = 50;
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);  // Starting gun
        CountDownLatch doneLatch = new CountDownLatch(numThreads);  // Finish line

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        System.out.println("Starting $1000 in account, 50 threads each trying to withdraw $50...");
        System.out.println("Expected: 20 succeed ($50 × 20 = $1000), 30 fail");

        // Submit all withdrawal tasks
        for (int i = 0; i < numThreads; i++) {
            final int txNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();  // Wait for the starting gun
                    Transaction tx = Transaction.create(
                            "CONC-" + txNum,
                            "CONCURRENT-001",
                            TransactionType.WITHDRAWAL,
                            withdrawAmount
                    );
                    ValidationResult result = validator.process(tx);
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

        // Wait for all threads to finish
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalBalance = validator.getAccount("CONCURRENT-001")
                .map(a -> a.getBalance())
                .orElse(BigDecimal.ZERO);

        System.out.println("\nResults:");
        System.out.println("  Successful withdrawals: " + successCount.get());
        System.out.println("  Failed withdrawals: " + failureCount.get());
        System.out.println("  Final balance: $" + finalBalance);
        System.out.println("  Balance is exactly $0: " +
                (finalBalance.compareTo(BigDecimal.ZERO) == 0 ? "✓ YES" : "✗ NO"));
    }

    /**
     * Demonstrates that only one of many concurrent duplicate submissions succeeds.
     */
    private static void demoConcurrentDuplicateDetection(TransactionValidator validator)
            throws InterruptedException {

        // Create a fresh account for this demo
        validator.createAccount("DUPLICATE-001", new BigDecimal("1000.00"));

        int numThreads = 20;
        String duplicateId = "SAME-TXN-ID";
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        System.out.println("20 threads simultaneously submitting the SAME transaction ID...");
        System.out.println("Expected: 1 succeeds, 19 detected as duplicates");

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Transaction tx = Transaction.create(
                            duplicateId,  // Same ID for all!
                            "DUPLICATE-001",
                            TransactionType.DEPOSIT,
                            new BigDecimal("10.00")
                    );
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
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("\nResults:");
        System.out.println("  Successful submissions: " + successCount.get());
        System.out.println("  Detected as duplicates: " + duplicateCount.get());
        System.out.println("  Only one succeeded: " +
                (successCount.get() == 1 ? "✓ YES" : "✗ NO"));
    }

    /**
     * Helper to print validation results in a consistent format.
     */
    private static void printResult(String operation, ValidationResult result) {
        String status = result.isSuccess() ? "✓" : "✗";
        System.out.printf("%s %-20s → %s%n", status, operation, result.message());
    }
}
