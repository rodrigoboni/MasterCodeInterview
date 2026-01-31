package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * PAUSED_CLOSING state: Door paused while closing.
 *
 * Transitions:
 * - BUTTON_PRESS -> CLOSING (resume)
 * - OBSTACLE -> (no change, motor not running)
 * - COMPLETE -> (no change, timer not running)
 *
 * Time is preserved in MovementData, so resuming continues
 * from where the door left off.
 */
public class PausedClosingState extends AbstractDoorState {

    /** Singleton instance */
    public static final PausedClosingState INSTANCE = new PausedClosingState();

    private PausedClosingState() {
        super("PAUSED_CLOSING");
    }

    @Override
    public boolean isPaused() {
        return true;
    }

    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        return ClosingState.INSTANCE; // Resume - time preserved in context
    }

    @Override
    public void onEnter(DoorContext context) {
        System.out.println("Door paused (was closing)");
    }
}
