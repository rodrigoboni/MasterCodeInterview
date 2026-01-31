package howToSolveCodingProblems.transactionValidator.validation;

import java.math.BigDecimal;

/**
 * Failed validation - insufficient funds for withdrawal/transfer.
 *
 * Contains both current balance and requested amount for informative error messages.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN INSIGHT: Rich Error Information
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Instead of just returning "false" or throwing a generic exception,
 * this record captures all relevant context:
 *
 *   - currentBalance: What the account actually has
 *   - requestedAmount: What was requested
 *   - shortfall(): Computed difference (how much more is needed)
 *
 * This enables:
 *   1. Detailed error messages to users
 *   2. Logging with full context
 *   3. UI can suggest "deposit $X more"
 *   4. Better debugging when issues occur
 *
 * INTERVIEW TIP:
 * ──────────────
 * Good error handling should tell the user:
 *   1. WHAT went wrong
 *   2. WHY it went wrong
 *   3. HOW to fix it (if possible)
 */
public record InsufficientFunds(
        BigDecimal currentBalance,
        BigDecimal requestedAmount
) implements ValidationResult {

    /**
     * Calculate the shortfall amount.
     *
     * @return How much additional funds are needed to complete the transaction
     */
    public BigDecimal shortfall() {
        return requestedAmount.subtract(currentBalance);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String message() {
        return String.format(
                "Insufficient funds. Current balance: %s, Requested: %s, Shortfall: %s",
                currentBalance, requestedAmount, shortfall()
        );
    }
}
