package howToSolveCodingProblems.stateMachines.doorController;

/**
 * Events that can trigger state transitions.
 *
 * - BUTTON_PRESS (P): User presses the wall button
 * - OBSTACLE (O): Obstacle sensor triggered (safety feature)
 * - COMPLETE: Movement finished (after 5 seconds elapsed)
 */
public enum DoorEvent {
    BUTTON_PRESS,  // P
    OBSTACLE,      // O
    COMPLETE       // Movement completed (internal event after 5 seconds)
}
