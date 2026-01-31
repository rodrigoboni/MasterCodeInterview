package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * OPENING state: Door is moving upward.
 *
 * Transitions:
 * - BUTTON_PRESS -> PAUSED_OPENING (pause)
 * - OBSTACLE -> CLOSING (reverse for safety)
 * - COMPLETE -> OPEN (finished opening)
 *
 * Extends AbstractMovingState to inherit time tracking behavior.
 * Only needs to specify the three transition states.
 */
public class OpeningState extends AbstractMovingState {

    /** Singleton instance */
    public static final OpeningState INSTANCE = new OpeningState();

    private OpeningState() {
        super("OPENING");
    }

    @Override
    protected DoorStateHandler getPausedState() {
        return PausedOpeningState.INSTANCE;
    }

    @Override
    protected DoorStateHandler getReverseState() {
        return ClosingState.INSTANCE;
    }

    @Override
    protected DoorStateHandler getCompletedState() {
        return OpenState.INSTANCE;
    }

    @Override
    public void onEnter(DoorContext context) {
        System.out.println("Door motor started: moving UP");
    }
}
