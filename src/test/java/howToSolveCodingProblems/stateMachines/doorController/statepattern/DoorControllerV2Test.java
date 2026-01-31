package howToSolveCodingProblems.stateMachines.doorController.statepattern;

import howToSolveCodingProblems.stateMachines.doorController.DoorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Door Controller V2 (State Pattern).
 *
 * These tests mirror DoorControllerTest to ensure behavioral equivalence
 * between V1 (switch-based) and V2 (State Pattern) implementations.
 */
class DoorControllerV2Test {

    private DoorControllerV2 door;

    @BeforeEach
    void setUp() {
        door = new DoorControllerV2();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("Door starts in CLOSED state")
        void doorStartsClosed() {
            assertEquals("CLOSED", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Door is not moving initially")
        void doorNotMovingInitially() {
            assertFalse(door.isMoving());
        }

        @Test
        @DisplayName("Door is not paused initially")
        void doorNotPausedInitially() {
            assertFalse(door.isPaused());
        }
    }

    @Nested
    @DisplayName("Button Press Transitions")
    class ButtonPressTransitions {

        @Test
        @DisplayName("CLOSED + P -> OPENING")
        void closedToOpening() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            assertEquals("OPENING", door.getCurrentStateName());
            assertTrue(door.isMoving());
        }

        @Test
        @DisplayName("OPENING + P -> PAUSED_OPENING")
        void openingToPausedOpening() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSED -> OPENING
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPENING -> PAUSED_OPENING
            assertEquals("PAUSED_OPENING", door.getCurrentStateName());
            assertTrue(door.isPaused());
        }

        @Test
        @DisplayName("PAUSED_OPENING + P -> OPENING (resume)")
        void pausedOpeningToOpening() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSED -> OPENING
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPENING -> PAUSED_OPENING
            door.processEvent(DoorEvent.BUTTON_PRESS); // PAUSED_OPENING -> OPENING
            assertEquals("OPENING", door.getCurrentStateName());
        }

        @Test
        @DisplayName("OPEN + P -> CLOSING")
        void openToClosing() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // Start opening
            door.advanceTime(5);                        // Complete opening
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPEN -> CLOSING
            assertEquals("CLOSING", door.getCurrentStateName());
        }

        @Test
        @DisplayName("CLOSING + P -> PAUSED_CLOSING")
        void closingToPausedClosing() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPEN -> CLOSING
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSING -> PAUSED_CLOSING
            assertEquals("PAUSED_CLOSING", door.getCurrentStateName());
        }

        @Test
        @DisplayName("PAUSED_CLOSING + P -> CLOSING (resume)")
        void pausedClosingToClosing() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.BUTTON_PRESS); // Resume
            assertEquals("CLOSING", door.getCurrentStateName());
        }
    }

    @Nested
    @DisplayName("Obstacle Detection Transitions")
    class ObstacleTransitions {

        @Test
        @DisplayName("OPENING + O -> CLOSING (reverse)")
        void openingToClosingOnObstacle() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals("CLOSING", door.getCurrentStateName());
        }

        @Test
        @DisplayName("CLOSING + O -> OPENING (safety reversal)")
        void closingToOpeningOnObstacle() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals("OPENING", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Obstacle has no effect when CLOSED")
        void obstacleIgnoredWhenClosed() {
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals("CLOSED", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Obstacle has no effect when OPEN")
        void obstacleIgnoredWhenOpen() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals("OPEN", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Obstacle has no effect when PAUSED")
        void obstacleIgnoredWhenPaused() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals("PAUSED_OPENING", door.getCurrentStateName());
        }
    }

    @Nested
    @DisplayName("Movement Completion Tests")
    class MovementCompletionTests {

        @Test
        @DisplayName("OPENING completes to OPEN after 5 seconds")
        void openingCompletesToOpen() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            boolean completed = door.advanceTime(5);
            assertTrue(completed);
            assertEquals("OPEN", door.getCurrentStateName());
        }

        @Test
        @DisplayName("CLOSING completes to CLOSED after 5 seconds")
        void closingCompletesToClosed() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            boolean completed = door.advanceTime(5);
            assertTrue(completed);
            assertEquals("CLOSED", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Partial movement tracked correctly")
        void partialMovementTracked() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(2);
            assertEquals(2, door.getElapsedTime());
            assertEquals(3, door.getRemainingTime());
            assertEquals("OPENING", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Time does not advance when paused")
        void timeDoesNotAdvanceWhenPaused() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(2);
            door.processEvent(DoorEvent.BUTTON_PRESS); // Pause
            int elapsedBefore = door.getElapsedTime();
            door.advanceTime(10); // Should have no effect
            assertEquals(elapsedBefore, door.getElapsedTime());
            assertEquals("PAUSED_OPENING", door.getCurrentStateName());
        }
    }

    @Nested
    @DisplayName("Full Cycle Tests")
    class FullCycleTests {

        @Test
        @DisplayName("Complete open-close cycle")
        void completeOpenCloseCycle() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            assertEquals("OPEN", door.getCurrentStateName());
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            assertEquals("CLOSED", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Pause, resume, and complete")
        void pauseResumeComplete() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(2);
            door.processEvent(DoorEvent.BUTTON_PRESS); // Pause
            door.processEvent(DoorEvent.BUTTON_PRESS); // Resume
            door.advanceTime(3);
            assertEquals("OPEN", door.getCurrentStateName());
        }

        @Test
        @DisplayName("Obstacle reversal during closing completes to OPEN")
        void obstacleReversalCompletesToOpen() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(2);
            door.processEvent(DoorEvent.OBSTACLE);
            door.advanceTime(2);
            assertEquals("OPEN", door.getCurrentStateName());
        }
    }

    @Nested
    @DisplayName("State Pattern Specific Tests")
    class StatePatternTests {

        @Test
        @DisplayName("States are singletons")
        void statesAreSingletons() {
            assertSame(ClosedState.INSTANCE, ClosedState.INSTANCE);
            assertSame(OpeningState.INSTANCE, OpeningState.INSTANCE);
            assertSame(OpenState.INSTANCE, OpenState.INSTANCE);
            assertSame(ClosingState.INSTANCE, ClosingState.INSTANCE);
        }

        @Test
        @DisplayName("Current state is correct instance")
        void currentStateIsCorrectInstance() {
            assertEquals(ClosedState.INSTANCE, door.getCurrentState());

            door.processEvent(DoorEvent.BUTTON_PRESS);
            assertEquals(OpeningState.INSTANCE, door.getCurrentState());

            door.advanceTime(5);
            assertEquals(OpenState.INSTANCE, door.getCurrentState());
        }

        @Test
        @DisplayName("Individual state can be tested in isolation")
        void individualStateTestable() {
            // Create a mock context
            MovementData movement = new MovementData();
            DoorContext mockContext = new DoorContext() {
                @Override
                public void processEvent(DoorEvent event) {}

                @Override
                public MovementData getMovementData() { return movement; }

                @Override
                public DoorStateHandler getCurrentState() { return ClosedState.INSTANCE; }
            };

            // Test ClosedState directly
            DoorStateHandler nextState = ClosedState.INSTANCE.onButtonPress(mockContext);
            assertEquals(OpeningState.INSTANCE, nextState);
        }
    }
}
