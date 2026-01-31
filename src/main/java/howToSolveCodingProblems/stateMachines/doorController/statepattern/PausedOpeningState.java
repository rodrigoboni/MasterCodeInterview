package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * PAUSED_OPENING state: Door paused while opening.
 *
 * Transitions:
 * - BUTTON_PRESS -> OPENING (resume)
 * - OBSTACLE -> (no change, motor not running)
 * - COMPLETE -> (no change, timer not running)
 *
 * Time is preserved in MovementData, so resuming continues
 * from where the door left off.
 */
public class PausedOpeningState extends AbstractDoorState {

    /** Singleton instance */
    public static final PausedOpeningState INSTANCE = new PausedOpeningState();

    private PausedOpeningState() {
        super("PAUSED_OPENING");
    }

    @Override
    public boolean isPaused() {
        return true;
    }

    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        return OpeningState.INSTANCE; // Resume - time preserved in context
    }

    @Override
    public void onEnter(DoorContext context) {
        System.out.println("Door paused (was opening)");
    }
}
