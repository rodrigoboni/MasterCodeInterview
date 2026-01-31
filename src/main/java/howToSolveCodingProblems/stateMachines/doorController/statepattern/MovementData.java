package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * Value object encapsulating movement timing data.
 *
 * This class applies the Strategy Pattern by encapsulating the time
 * tracking algorithm separately from state logic. This makes the
 * timing behavior testable in isolation and potentially replaceable.
 *
 * Design Benefits:
 * - Single Responsibility: Only handles time tracking
 * - Encapsulation: Hides time calculation details from states
 * - Testability: Can be unit tested independently
 */
public class MovementData {

    public static final int FULL_MOVEMENT_TIME = 5; // seconds

    private int elapsedTime;
    private int remainingTime;

    public MovementData() {
        reset();
    }

    /**
     * Reset for a new full movement (5 seconds).
     */
    public void reset() {
        this.elapsedTime = 0;
        this.remainingTime = FULL_MOVEMENT_TIME;
    }

    /**
     * Advance time during movement.
     * @param seconds Time to advance
     */
    public void advanceTime(int seconds) {
        this.elapsedTime += seconds;
        this.remainingTime -= seconds;
        if (this.remainingTime < 0) {
            this.remainingTime = 0;
        }
    }

    /**
     * Reverse direction - swap elapsed/remaining for continuity.
     * When door reverses mid-movement, it should take the same amount
     * of time to return as it has already traveled.
     */
    public void reverse() {
        int temp = elapsedTime;
        elapsedTime = FULL_MOVEMENT_TIME - remainingTime;
        remainingTime = Math.max(temp, 1); // At least 1 second to move
    }

    /**
     * @return true if movement is complete (remaining time is zero or less)
     */
    public boolean isComplete() {
        return remainingTime <= 0;
    }

    public int getElapsedTime() {
        return elapsedTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    @Override
    public String toString() {
        return String.format("elapsed=%ds, remaining=%ds", elapsedTime, remainingTime);
    }
}
