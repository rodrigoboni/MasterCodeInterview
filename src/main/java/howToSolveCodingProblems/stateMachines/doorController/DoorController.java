package howToSolveCodingProblems.stateMachines.doorController;

/**
 * Door Controller State Machine
 *
 * Implements a garage door controller that:
 * - Takes 5 seconds to fully open/close
 * - Can be paused/resumed with button press
 * - Reverses direction when obstacle detected during movement
 *
 * State Transition Table:
 * ┌─────────────────┬──────────────┬──────────────┬──────────────┐
 * │ Current State   │ BUTTON_PRESS │ OBSTACLE     │ COMPLETE     │
 * ├─────────────────┼──────────────┼──────────────┼──────────────┤
 * │ CLOSED          │ OPENING      │ (no change)  │ (no change)  │
 * │ OPENING         │ PAUSED_OPEN  │ CLOSING      │ OPEN         │
 * │ OPEN            │ CLOSING      │ (no change)  │ (no change)  │
 * │ CLOSING         │ PAUSED_CLOSE │ OPENING      │ CLOSED       │
 * │ PAUSED_OPENING  │ OPENING      │ (no change)  │ (no change)  │
 * │ PAUSED_CLOSING  │ CLOSING      │ (no change)  │ (no change)  │
 * └─────────────────┴──────────────┴──────────────┴──────────────┘
 */
public class DoorController {

    private static final int FULL_MOVEMENT_TIME = 5; // seconds to fully open/close

    private DoorState currentState;
    private int elapsedTime;      // Time spent in current movement direction
    private int remainingTime;    // Time remaining to complete movement

    public DoorController() {
        this.currentState = DoorState.CLOSED;
        this.elapsedTime = 0;
        this.remainingTime = FULL_MOVEMENT_TIME;
    }

    /**
     * Process an event and transition to the next state.
     *
     * @param event The event to process
     * @return The new state after processing the event
     */
    public DoorState processEvent(DoorEvent event) {
        DoorState previousState = currentState;

        switch (currentState) {
            case CLOSED:
                handleClosedState(event);
                break;
            case OPENING:
                handleOpeningState(event);
                break;
            case OPEN:
                handleOpenState(event);
                break;
            case CLOSING:
                handleClosingState(event);
                break;
            case PAUSED_OPENING:
                handlePausedOpeningState(event);
                break;
            case PAUSED_CLOSING:
                handlePausedClosingState(event);
                break;
        }

        // Log state transition for debugging
        if (previousState != currentState) {
            System.out.printf("Transition: %s --[%s]--> %s%n",
                previousState, event, currentState);
        }

        return currentState;
    }

    private void handleClosedState(DoorEvent event) {
        if (event == DoorEvent.BUTTON_PRESS) {
            currentState = DoorState.OPENING;
            elapsedTime = 0;
            remainingTime = FULL_MOVEMENT_TIME;
        }
        // OBSTACLE and COMPLETE have no effect when closed
    }

    private void handleOpeningState(DoorEvent event) {
        switch (event) {
            case BUTTON_PRESS:
                // Pause the opening motion
                currentState = DoorState.PAUSED_OPENING;
                break;
            case OBSTACLE:
                // Reverse direction - start closing from current position
                currentState = DoorState.CLOSING;
                // Swap times: remaining becomes elapsed for reverse direction
                int temp = elapsedTime;
                elapsedTime = FULL_MOVEMENT_TIME - remainingTime;
                remainingTime = temp > 0 ? temp : FULL_MOVEMENT_TIME;
                break;
            case COMPLETE:
                // Door fully opened
                currentState = DoorState.OPEN;
                elapsedTime = FULL_MOVEMENT_TIME;
                remainingTime = 0;
                break;
        }
    }

    private void handleOpenState(DoorEvent event) {
        if (event == DoorEvent.BUTTON_PRESS) {
            currentState = DoorState.CLOSING;
            elapsedTime = 0;
            remainingTime = FULL_MOVEMENT_TIME;
        }
        // OBSTACLE and COMPLETE have no effect when fully open
    }

    private void handleClosingState(DoorEvent event) {
        switch (event) {
            case BUTTON_PRESS:
                // Pause the closing motion
                currentState = DoorState.PAUSED_CLOSING;
                break;
            case OBSTACLE:
                // Safety feature: reverse direction immediately
                currentState = DoorState.OPENING;
                // Swap times for reverse direction
                int temp = elapsedTime;
                elapsedTime = FULL_MOVEMENT_TIME - remainingTime;
                remainingTime = temp > 0 ? temp : FULL_MOVEMENT_TIME;
                break;
            case COMPLETE:
                // Door fully closed
                currentState = DoorState.CLOSED;
                elapsedTime = 0;
                remainingTime = FULL_MOVEMENT_TIME;
                break;
        }
    }

    private void handlePausedOpeningState(DoorEvent event) {
        if (event == DoorEvent.BUTTON_PRESS) {
            // Resume opening from where we left off
            currentState = DoorState.OPENING;
        }
        // OBSTACLE has no effect when paused (motor not running)
        // COMPLETE has no effect when paused (timer not running)
    }

    private void handlePausedClosingState(DoorEvent event) {
        if (event == DoorEvent.BUTTON_PRESS) {
            // Resume closing from where we left off
            currentState = DoorState.CLOSING;
        }
        // OBSTACLE has no effect when paused (motor not running)
        // COMPLETE has no effect when paused (timer not running)
    }

    /**
     * Simulate time passing. Call this method to advance the timer.
     *
     * @param seconds Number of seconds that have passed
     * @return true if movement completed during this time period
     */
    public boolean advanceTime(int seconds) {
        if (currentState != DoorState.OPENING && currentState != DoorState.CLOSING) {
            return false; // Timer only runs during active movement
        }

        elapsedTime += seconds;
        remainingTime -= seconds;

        if (remainingTime <= 0) {
            remainingTime = 0;
            processEvent(DoorEvent.COMPLETE);
            return true;
        }

        return false;
    }

    // Getters for state inspection

    public DoorState getCurrentState() {
        return currentState;
    }

    public int getElapsedTime() {
        return elapsedTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public boolean isMoving() {
        return currentState == DoorState.OPENING || currentState == DoorState.CLOSING;
    }

    public boolean isPaused() {
        return currentState == DoorState.PAUSED_OPENING || currentState == DoorState.PAUSED_CLOSING;
    }

    @Override
    public String toString() {
        return String.format("DoorController[state=%s, elapsed=%ds, remaining=%ds]",
            currentState, elapsedTime, remainingTime);
    }
}
