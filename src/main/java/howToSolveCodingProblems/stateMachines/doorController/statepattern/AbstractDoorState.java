package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * Abstract base class providing default "no-op" implementations.
 *
 * States only need to override the events they actually handle.
 * This reduces boilerplate and makes each state's responsibilities clear.
 *
 * Design Pattern: Template Method (partial)
 * - Provides default behavior that subclasses can override
 * - Common functionality (toString, getStateName) implemented once
 */
public abstract class AbstractDoorState implements DoorStateHandler {

    private final String stateName;

    protected AbstractDoorState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        return this; // Default: no transition
    }

    @Override
    public DoorStateHandler onObstacle(DoorContext context) {
        return this; // Default: no transition
    }

    @Override
    public DoorStateHandler onComplete(DoorContext context) {
        return this; // Default: no transition
    }

    @Override
    public boolean isMoving() {
        return false; // Default: not moving
    }

    @Override
    public boolean isPaused() {
        return false; // Default: not paused
    }

    @Override
    public String getStateName() {
        return stateName;
    }

    @Override
    public String toString() {
        return stateName;
    }
}
