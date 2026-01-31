package howToSolveCodingProblems.transactionValidator.validation;

/**
 * Failed validation - transaction ID has already been processed.
 *
 * This prevents replay attacks and accidental duplicate submissions.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * CONCEPT: Idempotency in Distributed Systems
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS IDEMPOTENCY?
 * ────────────────────
 * An operation is idempotent if performing it multiple times has the same
 * effect as performing it once.
 *
 *   Idempotent:     GET /users/123      (returns same user each time)
 *   Idempotent:     DELETE /users/123   (user is deleted first time, no-op after)
 *   NOT Idempotent: POST /transactions  (creates new transaction each time!)
 *
 * WHY DOES IT MATTER?
 * ───────────────────
 * In distributed systems, network issues can cause:
 *   1. Client doesn't receive response (timeout)
 *   2. Client retries the request
 *   3. Server receives the same request twice
 *
 * Without idempotency:
 *   - User charged twice for same purchase
 *   - Money transferred twice
 *   - Inventory decremented twice
 *
 * HOW WE ACHIEVE IDEMPOTENCY:
 * ───────────────────────────
 *   1. Client generates unique transaction ID before sending
 *   2. Server tracks all processed transaction IDs
 *   3. If ID was seen before → return DuplicateTransaction
 *   4. If ID is new → process and remember the ID
 *
 * STORAGE CONSIDERATIONS:
 * ───────────────────────
 * Our ConcurrentHashMap stores IDs forever. In production, you'd want:
 *   - TTL (Time-To-Live) to expire old entries
 *   - External storage (Redis, database) for durability
 *   - Sharding for high-volume systems
 *
 * INTERVIEW TIP:
 * ──────────────
 * Always discuss idempotency when designing APIs that modify state.
 * It shows you understand real-world distributed systems challenges.
 */
public record DuplicateTransaction(String transactionId) implements ValidationResult {

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String message() {
        return "Duplicate transaction rejected. ID: " + transactionId;
    }
}
