package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * CLOSING state: Door is moving downward.
 *
 * Transitions:
 * - BUTTON_PRESS -> PAUSED_CLOSING (pause)
 * - OBSTACLE -> OPENING (safety reversal!)
 * - COMPLETE -> CLOSED (finished closing)
 *
 * The obstacle reversal is a critical safety feature in real garage doors
 * to prevent crushing objects (or people) under the closing door.
 */
public class ClosingState extends AbstractMovingState {

    /** Singleton instance */
    public static final ClosingState INSTANCE = new ClosingState();

    private ClosingState() {
        super("CLOSING");
    }

    @Override
    protected DoorStateHandler getPausedState() {
        return PausedClosingState.INSTANCE;
    }

    @Override
    protected DoorStateHandler getReverseState() {
        return OpeningState.INSTANCE;
    }

    @Override
    protected DoorStateHandler getCompletedState() {
        return ClosedState.INSTANCE;
    }

    @Override
    public void onEnter(DoorContext context) {
        System.out.println("Door motor started: moving DOWN");
    }
}
