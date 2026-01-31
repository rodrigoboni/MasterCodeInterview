package howToSolveCodingProblems.stateMachines.doorController;

/**
 * Demonstration of the Door Controller State Machine.
 *
 * This class simulates various scenarios to show how the state machine
 * handles different event sequences.
 */
public class DoorControllerDemo {

    public static void main(String[] args) {
        System.out.println("=== Door Controller State Machine Demo ===\n");

        scenario1_NormalOperation();
        scenario2_PauseResume();
        scenario3_ObstacleReversal();
        scenario4_ComplexSequence();
    }

    /**
     * Scenario 1: Normal open/close cycle
     * Events: P (open) -> wait 5s -> P (close) -> wait 5s
     */
    private static void scenario1_NormalOperation() {
        System.out.println("--- Scenario 1: Normal Operation ---");
        DoorController door = new DoorController();
        System.out.println("Initial: " + door);

        // Press button to open
        door.processEvent(DoorEvent.BUTTON_PRESS);
        System.out.println("After P: " + door);

        // Wait for door to fully open
        door.advanceTime(5);
        System.out.println("After 5s: " + door);

        // Press button to close
        door.processEvent(DoorEvent.BUTTON_PRESS);
        System.out.println("After P: " + door);

        // Wait for door to fully close
        door.advanceTime(5);
        System.out.println("After 5s: " + door);

        System.out.println();
    }

    /**
     * Scenario 2: Pause and resume during opening
     * Events: P (open) -> wait 2s -> P (pause) -> P (resume) -> wait 3s
     */
    private static void scenario2_PauseResume() {
        System.out.println("--- Scenario 2: Pause/Resume ---");
        DoorController door = new DoorController();

        // Start opening
        door.processEvent(DoorEvent.BUTTON_PRESS);
        System.out.println("Started opening: " + door);

        // Wait 2 seconds
        door.advanceTime(2);
        System.out.println("After 2s: " + door);

        // Pause
        door.processEvent(DoorEvent.BUTTON_PRESS);
        System.out.println("Paused: " + door);

        // Resume
        door.processEvent(DoorEvent.BUTTON_PRESS);
        System.out.println("Resumed: " + door);

        // Wait remaining 3 seconds
        door.advanceTime(3);
        System.out.println("After 3s: " + door);

        System.out.println();
    }

    /**
     * Scenario 3: Obstacle causes reversal during closing
     * Events: Start open -> complete -> start close -> obstacle -> complete
     */
    private static void scenario3_ObstacleReversal() {
        System.out.println("--- Scenario 3: Obstacle Reversal ---");
        DoorController door = new DoorController();

        // Open the door fully
        door.processEvent(DoorEvent.BUTTON_PRESS);
        door.advanceTime(5);
        System.out.println("Door fully open: " + door);

        // Start closing
        door.processEvent(DoorEvent.BUTTON_PRESS);
        System.out.println("Started closing: " + door);

        // After 2 seconds, obstacle detected!
        door.advanceTime(2);
        System.out.println("After 2s closing: " + door);

        door.processEvent(DoorEvent.OBSTACLE);
        System.out.println("OBSTACLE! Reversed: " + door);

        // Door should now open (taking ~2s to get back to fully open)
        door.advanceTime(2);
        System.out.println("After 2s opening: " + door);

        System.out.println();
    }

    /**
     * Scenario 4: Complex sequence with string input
     * Simulates processing a string like "PPOPPP"
     */
    private static void scenario4_ComplexSequence() {
        System.out.println("--- Scenario 4: Processing Event String ---");
        String events = "PPOPPP";
        System.out.println("Event sequence: " + events);
        System.out.println();

        DoorController door = new DoorController();
        System.out.println("Initial: " + door.getCurrentState());

        for (char c : events.toCharArray()) {
            DoorEvent event = charToEvent(c);
            if (event != null) {
                System.out.printf("Processing '%c' (%s)...%n", c, event);
                door.processEvent(event);
                System.out.println("  -> State: " + door.getCurrentState());
            }
        }

        System.out.println("\nFinal: " + door);
    }

    /**
     * Convert character to DoorEvent.
     * P -> BUTTON_PRESS
     * O -> OBSTACLE
     */
    private static DoorEvent charToEvent(char c) {
        switch (Character.toUpperCase(c)) {
            case 'P':
                return DoorEvent.BUTTON_PRESS;
            case 'O':
                return DoorEvent.OBSTACLE;
            default:
                return null;
        }
    }
}
