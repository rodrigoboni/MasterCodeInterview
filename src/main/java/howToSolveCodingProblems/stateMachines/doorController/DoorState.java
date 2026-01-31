package howToSolveCodingProblems.stateMachines.doorController;

/**
 * Enumeration of all possible door states.
 *
 * State Machine States:
 * - CLOSED: Door is fully closed (initial state)
 * - OPENING: Door is moving upward (takes 5 seconds)
 * - OPEN: Door is fully open
 * - CLOSING: Door is moving downward (takes 5 seconds)
 * - PAUSED_OPENING: Door paused while opening
 * - PAUSED_CLOSING: Door paused while closing
 */
public enum DoorState {
    CLOSED,
    OPENING,
    OPEN,
    CLOSING,
    PAUSED_OPENING,
    PAUSED_CLOSING
}
