package howToSolveCodingProblems.stateMachines.doorController.statepattern;

import howToSolveCodingProblems.stateMachines.doorController.DoorEvent;

/**
 * Context interface for the State Pattern.
 *
 * States use this interface to interact with the controller without
 * being tightly coupled to its concrete implementation. This follows
 * the Dependency Inversion Principle.
 */
public interface DoorContext {

    /**
     * Process an event (used internally for triggering COMPLETE).
     * @param event The event to process
     */
    void processEvent(DoorEvent event);

    /**
     * Get the movement data for time tracking.
     * @return The movement data object
     */
    MovementData getMovementData();

    /**
     * Get the current state handler.
     * @return The current state
     */
    DoorStateHandler getCurrentState();
}
