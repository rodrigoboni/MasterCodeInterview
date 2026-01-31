package howToSolveCodingProblems.stateMachines.doorController.statepattern;

import howToSolveCodingProblems.stateMachines.doorController.DoorEvent;

/**
 * Abstract class for states where the door is actively moving (OPENING, CLOSING).
 *
 * This class applies the Template Method Pattern:
 * - The advanceTime() method defines the algorithm skeleton
 * - Subclasses provide specific states via abstract methods:
 *   - getPausedState(): What state to enter when paused
 *   - getReverseState(): What state to enter on obstacle
 *   - getCompletedState(): What state to enter when movement finishes
 *
 * Benefits:
 * - DRY: Time advancement logic written once
 * - Consistent: All moving states behave the same for time tracking
 * - Extensible: New moving states just implement 3 abstract methods
 */
public abstract class AbstractMovingState extends AbstractDoorState {

    protected AbstractMovingState(String stateName) {
        super(stateName);
    }

    @Override
    public final boolean isMoving() {
        return true;
    }

    /**
     * Template Method for time advancement.
     * Defines the algorithm: advance time, check completion, trigger event.
     */
    @Override
    public boolean advanceTime(DoorContext context, int seconds) {
        MovementData movement = context.getMovementData();
        movement.advanceTime(seconds);

        if (movement.isComplete()) {
            context.processEvent(DoorEvent.COMPLETE);
            return true;
        }
        return false;
    }

    /**
     * @return The state to transition to when paused
     */
    protected abstract DoorStateHandler getPausedState();

    /**
     * @return The state to transition to on obstacle (reverse direction)
     */
    protected abstract DoorStateHandler getReverseState();

    /**
     * @return The state to transition to when movement completes
     */
    protected abstract DoorStateHandler getCompletedState();

    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        return getPausedState();
    }

    @Override
    public DoorStateHandler onObstacle(DoorContext context) {
        // Reverse direction - swap elapsed/remaining for continuity
        context.getMovementData().reverse();
        return getReverseState();
    }

    @Override
    public DoorStateHandler onComplete(DoorContext context) {
        return getCompletedState();
    }
}
