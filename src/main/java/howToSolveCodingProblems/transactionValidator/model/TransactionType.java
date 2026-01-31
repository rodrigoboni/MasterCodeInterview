package howToSolveCodingProblems.transactionValidator.model;

/**
 * Enum representing the type of financial transaction.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERN: Enum Type Pattern
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * WHY USE AN ENUM INSTEAD OF STRING CONSTANTS?
 * ─────────────────────────────────────────────
 *   1. TYPE SAFETY - Compiler catches invalid values at compile time
 *      - With String: processTransaction("WITHDRAWL") // typo compiles!
 *      - With Enum: processTransaction(TransactionType.WITHDRAWL) // won't compile
 *
 *   2. SWITCH EXHAUSTIVENESS - Compiler warns if you miss a case
 *      switch(type) {
 *          case DEPOSIT -> ...
 *          case WITHDRAWAL -> ...
 *          // Compiler warns: TRANSFER not handled!
 *      }
 *
 *   3. THREAD SAFETY - Enums are inherently immutable and thread-safe
 *      - Singleton per value (JVM guarantees only one DEPOSIT instance)
 *      - No synchronization needed when sharing between threads
 *
 *   4. SERIALIZATION SAFETY - Built-in serialization support
 *      - Prevents deserialization attacks that create fake enum instances
 *
 * INTERVIEW INSIGHT:
 * ──────────────────
 * Enums in Java are more powerful than in most languages:
 *   - Can have methods, fields, and constructors
 *   - Can implement interfaces
 *   - Are implicitly final and extend java.lang.Enum
 *   - valueOf() and values() are auto-generated
 */
public enum TransactionType {

    /**
     * Adding funds to an account.
     * Always succeeds (no balance check needed).
     */
    DEPOSIT,

    /**
     * Removing funds from an account.
     * Requires balance validation before execution.
     */
    WITHDRAWAL,

    /**
     * Moving funds between accounts.
     * Conceptually: WITHDRAWAL from source + DEPOSIT to target.
     * In this implementation, we treat it as a withdrawal from the source account.
     */
    TRANSFER
}
