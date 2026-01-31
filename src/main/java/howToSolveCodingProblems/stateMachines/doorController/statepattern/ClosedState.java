package howToSolveCodingProblems.stateMachines.doorController.statepattern;

/**
 * CLOSED state: Door is fully closed.
 *
 * Transitions:
 * - BUTTON_PRESS -> OPENING (start opening)
 * - OBSTACLE -> (no change, door is closed)
 * - COMPLETE -> (no change, not moving)
 *
 * This class uses the Singleton Pattern since the state is stateless
 * (no instance variables that change). All CLOSED behavior is identical,
 * so we can share a single instance.
 */
public class ClosedState extends AbstractDoorState {

    /** Singleton instance - safe because state is stateless */
    public static final ClosedState INSTANCE = new ClosedState();

    private ClosedState() {
        super("CLOSED");
    }

    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        context.getMovementData().reset();
        return OpeningState.INSTANCE;
    }

    @Override
    public void onEnter(DoorContext context) {
        System.out.println("Door fully closed");
    }
}
