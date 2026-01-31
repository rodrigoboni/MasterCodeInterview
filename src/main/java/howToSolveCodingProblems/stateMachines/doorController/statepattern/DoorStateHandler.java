package howToSolveCodingProblems.stateMachines.doorController.statepattern;

import howToSolveCodingProblems.stateMachines.doorController.DoorEvent;

/**
 * State Pattern interface for door states.
 *
 * Each concrete state implements this interface, defining its own
 * event handling behavior. This eliminates switch statements by
 * using polymorphic dispatch.
 *
 * @see <a href="https://refactoring.guru/design-patterns/state">State Pattern</a>
 */
public interface DoorStateHandler {

    /**
     * Handle button press event.
     * @param context The door controller context
     * @return The next state (can return 'this' for no transition)
     */
    DoorStateHandler onButtonPress(DoorContext context);

    /**
     * Handle obstacle detection event.
     * @param context The door controller context
     * @return The next state (can return 'this' for no transition)
     */
    DoorStateHandler onObstacle(DoorContext context);

    /**
     * Handle movement completion event.
     * @param context The door controller context
     * @return The next state (can return 'this' for no transition)
     */
    DoorStateHandler onComplete(DoorContext context);

    /**
     * Called when entering this state (entry action).
     * @param context The door controller context
     */
    default void onEnter(DoorContext context) {}

    /**
     * Called when exiting this state (exit action).
     * @param context The door controller context
     */
    default void onExit(DoorContext context) {}

    /**
     * Process time advancement (only relevant for moving states).
     * @param context The door controller context
     * @param seconds Time elapsed in seconds
     * @return true if movement completed during this time period
     */
    default boolean advanceTime(DoorContext context, int seconds) {
        return false; // Default: time advancement has no effect
    }

    /**
     * @return true if door is actively moving in this state
     */
    boolean isMoving();

    /**
     * @return true if door is paused in this state
     */
    boolean isPaused();

    /**
     * @return The state name for logging/debugging
     */
    String getStateName();
}
