package howToSolveCodingProblems.transactionValidator.validation;

import java.math.BigDecimal;

/**
 * Successful validation - transaction can proceed.
 *
 * Contains the new balance after the transaction would be applied.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERN: Value Object (using Java Record)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * A Value Object is defined by its attributes rather than its identity.
 * Two Success objects with the same newBalance are considered equal.
 *
 * Records automatically implement:
 *   - equals() based on all fields
 *   - hashCode() based on all fields
 *   - toString() with all fields
 *
 * This makes them perfect for representing validation results.
 */
public record Success(BigDecimal newBalance) implements ValidationResult {

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public String message() {
        return "Transaction successful. New balance: " + newBalance;
    }
}
