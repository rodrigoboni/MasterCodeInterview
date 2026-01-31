package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * OPEN state: Door is fully open.
 *
 * Transitions:
 * - BUTTON_PRESS -> CLOSING (start closing)
 * - OBSTACLE -> (no change, door is open)
 * - COMPLETE -> (no change, not moving)
 *
 * Singleton Pattern: Stateless, single instance shared.
 */
public class OpenState extends AbstractDoorState {

    /** Singleton instance */
    public static final OpenState INSTANCE = new OpenState();

    private OpenState() {
        super("OPEN");
    }

    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        context.getMovementData().reset();
        return ClosingState.INSTANCE;
    }

    @Override
    public void onEnter(DoorContext context) {
        System.out.println("Door fully open");
    }
}
