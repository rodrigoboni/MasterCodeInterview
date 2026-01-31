package howToSolveCodingProblems.stateMachines.doorController.statepattern;

import howToSolveCodingProblems.stateMachines.doorController.DoorEvent;

/**
 * Door Controller using State Pattern (Version 2).
 *
 * This implementation eliminates switch statements by delegating
 * event handling to polymorphic state objects. Each state is
 * responsible for its own transitions.
 *
 * ═══════════════════════════════════════════════════════════════
 * DESIGN PATTERNS USED:
 * ═══════════════════════════════════════════════════════════════
 *
 * 1. STATE PATTERN
 *    - Each DoorState is a class implementing DoorStateHandler
 *    - State-specific behavior encapsulated in state classes
 *    - Controller delegates to current state for all event handling
 *
 * 2. TEMPLATE METHOD PATTERN
 *    - AbstractMovingState.advanceTime() defines the algorithm
 *    - Subclasses (OpeningState, ClosingState) provide specific states
 *
 * 3. SINGLETON PATTERN
 *    - State instances are stateless and shared (e.g., ClosedState.INSTANCE)
 *    - Safe because states don't hold mutable instance data
 *
 * 4. STRATEGY PATTERN
 *    - MovementData encapsulates timing strategy
 *    - Could be replaced with different timing implementations
 *
 * ═══════════════════════════════════════════════════════════════
 * COMPARISON WITH V1 (Switch-Based):
 * ═══════════════════════════════════════════════════════════════
 *
 * V1 (DoorController):
 *   switch (currentState) {
 *       case CLOSED: handleClosedState(event); break;
 *       case OPENING: handleOpeningState(event); break;
 *       // ... 6 cases total
 *   }
 *
 * V2 (DoorControllerV2 - This class):
 *   DoorStateHandler nextState = currentState.onButtonPress(this);
 *   // Pure polymorphic dispatch - no switch needed!
 *
 * Benefits:
 * - Adding new state = create 1 class (Open/Closed Principle)
 * - Each state testable in isolation
 * - State behavior colocated with state identity
 * - No scattered handler methods
 */
public class DoorControllerV2 implements DoorContext {

    private DoorStateHandler currentState;
    private final MovementData movementData;

    public DoorControllerV2() {
        this.movementData = new MovementData();
        this.currentState = ClosedState.INSTANCE;
    }

    /**
     * Process an event and transition to the next state.
     *
     * Notice: NO SWITCH STATEMENTS on state!
     * The polymorphic dispatch happens when we call currentState.onXxx()
     */
    @Override
    public void processEvent(DoorEvent event) {
        DoorStateHandler previousState = currentState;

        // Polymorphic dispatch - each state handles its own transitions
        DoorStateHandler nextState = switch (event) {
            case BUTTON_PRESS -> currentState.onButtonPress(this);
            case OBSTACLE -> currentState.onObstacle(this);
            case COMPLETE -> currentState.onComplete(this);
        };

        // Handle state transition with entry/exit actions
        if (nextState != previousState) {
            previousState.onExit(this);
            currentState = nextState;
            currentState.onEnter(this);

            System.out.printf("Transition: %s --[%s]--> %s%n",
                previousState.getStateName(), event, currentState.getStateName());
        }
    }

    /**
     * Advance time - delegates to current state.
     * Only moving states actually process time.
     */
    public boolean advanceTime(int seconds) {
        return currentState.advanceTime(this, seconds);
    }

    // ═══════════════════════════════════════════════════════════════
    // DoorContext interface implementation
    // ═══════════════════════════════════════════════════════════════

    @Override
    public MovementData getMovementData() {
        return movementData;
    }

    @Override
    public DoorStateHandler getCurrentState() {
        return currentState;
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API (matches V1 interface for compatibility)
    // ═══════════════════════════════════════════════════════════════

    public String getCurrentStateName() {
        return currentState.getStateName();
    }

    public int getElapsedTime() {
        return movementData.getElapsedTime();
    }

    public int getRemainingTime() {
        return movementData.getRemainingTime();
    }

    public boolean isMoving() {
        return currentState.isMoving();
    }

    public boolean isPaused() {
        return currentState.isPaused();
    }

    @Override
    public String toString() {
        return String.format("DoorControllerV2[state=%s, %s]",
            currentState.getStateName(), movementData);
    }
}
