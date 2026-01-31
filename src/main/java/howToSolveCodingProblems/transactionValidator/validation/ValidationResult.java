package howToSolveCodingProblems.transactionValidator.validation;

/**
 * Sealed interface for validation results enabling exhaustive pattern matching.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERN: Result Pattern (also called "Either" in functional programming)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * The Result Pattern represents operations that can either succeed or fail.
 * It's an alternative to exceptions for EXPECTED failures.
 *
 * COMPARISON: Result Pattern vs Exceptions
 * ─────────────────────────────────────────
 *
 *   ┌────────────────────┬───────────────────────────┬─────────────────────────┐
 *   │ Aspect             │ Exceptions                │ Result Pattern          │
 *   ├────────────────────┼───────────────────────────┼─────────────────────────┤
 *   │ Type signature     │ Hidden (throws clause)    │ Explicit in return type │
 *   │ Performance        │ Stack trace overhead      │ No overhead             │
 *   │ Handling           │ Can be ignored (unchecked)│ Must handle all cases   │
 *   │ Control flow       │ Non-local jumps           │ Local, explicit         │
 *   │ Best for           │ Unexpected errors         │ Expected failures       │
 *   └────────────────────┴───────────────────────────┴─────────────────────────┘
 *
 * WHEN TO USE EACH:
 *
 *   Use Result Pattern for:
 *   - Validation failures (invalid input, insufficient funds)
 *   - Business rule violations
 *   - Expected "alternative paths"
 *
 *   Use Exceptions for:
 *   - Programming errors (null pointers, index out of bounds)
 *   - Resource failures (disk full, network down)
 *   - Unexpected system errors
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * JAVA FEATURE: Sealed Classes/Interfaces (Java 17+)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * A sealed type restricts which classes can extend/implement it.
 *
 * SYNTAX:
 *   sealed interface ValidationResult
 *       permits Success, InsufficientFunds, DuplicateTransaction, InvalidInput
 *
 * WHY SEALED?
 *
 *   1. EXHAUSTIVE PATTERN MATCHING (Java 21+)
 *      The compiler knows ALL possible implementations, so switch doesn't need default:
 *
 *      switch (result) {
 *          case Success s -> handleSuccess(s);
 *          case InsufficientFunds f -> handleInsufficientFunds(f);
 *          case DuplicateTransaction d -> handleDuplicate(d);
 *          case InvalidInput i -> handleInvalid(i);
 *          // No default needed! Compiler knows these are ALL cases.
 *      }
 *
 *   2. CLOSED DOMAIN MODEL
 *      No surprises at runtime - you control all possible outcomes.
 *      Third-party code cannot add unexpected implementations.
 *
 *   3. REFACTORING SAFETY
 *      If you add a new case (e.g., TimeoutError), the compiler will
 *      flag EVERY switch that doesn't handle it (in Java 21+).
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * JAVA FEATURE: Pattern Matching for instanceof (Java 17)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Pattern matching combines instanceof check with variable binding:
 *
 *   // OLD WAY (before Java 17):
 *   if (result instanceof Success) {
 *       Success s = (Success) result;
 *       System.out.println(s.newBalance());
 *   }
 *
 *   // NEW WAY (Java 17+):
 *   if (result instanceof Success s) {
 *       System.out.println(s.newBalance());
 *   }
 *
 * INTERVIEW TIP:
 * ──────────────
 * Sealed interfaces + pattern matching is a modern Java idiom that shows
 * you understand:
 *   - Algebraic data types (ADTs) from functional programming
 *   - Type-safe error handling
 *   - Modern Java features
 */
public sealed interface ValidationResult
        permits Success, InsufficientFunds, DuplicateTransaction, InvalidInput {

    /**
     * Check if the validation passed.
     *
     * @return true if validation succeeded and transaction can proceed
     */
    boolean isSuccess();

    /**
     * Get a human-readable description of the result.
     *
     * @return Description suitable for logging or user display
     */
    String message();
}
