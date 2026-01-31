package howToSolveCodingProblems.transactionValidator.validation;

/**
 * Failed validation - invalid input data.
 *
 * Covers cases like null transaction, unknown account, etc.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PRINCIPLE: Fail Fast
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS FAIL FAST?
 * ──────────────────
 * Detect and report errors as early as possible, rather than letting them
 * propagate through the system and cause harder-to-diagnose failures later.
 *
 * EXAMPLES IN THIS SYSTEM:
 * ────────────────────────
 *   1. Transaction record validates in constructor
 *      → Fails immediately if amount is negative
 *      → No invalid Transaction object can ever exist
 *
 *   2. TransactionValidator.process() checks null first
 *      → Fails before attempting any database/lock operations
 *      → Clear error message instead of NullPointerException
 *
 *   3. Account lookup happens before business logic
 *      → Fails with "Account not found" instead of cryptic errors
 *
 * WHY FAIL FAST IS BETTER:
 * ────────────────────────
 *   ┌────────────────────────┬────────────────────────────────────┐
 *   │ Fail Fast              │ Fail Late                          │
 *   ├────────────────────────┼────────────────────────────────────┤
 *   │ Clear error message    │ Cryptic stack trace                │
 *   │ Error at input point   │ Error deep in business logic       │
 *   │ Easy to debug          │ Hard to trace back to root cause   │
 *   │ No partial operations  │ May leave system in bad state      │
 *   │ Predictable behavior   │ Unpredictable side effects         │
 *   └────────────────────────┴────────────────────────────────────┘
 *
 * INTERVIEW TIP:
 * ──────────────
 * When reviewing code or designing systems, always ask:
 * "Where can this fail, and how early can we detect it?"
 */
public record InvalidInput(String reason) implements ValidationResult {

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String message() {
        return "Invalid input: " + reason;
    }
}
