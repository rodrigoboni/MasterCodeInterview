package howToSolveCodingProblems.stateMachines.doorController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Door Controller State Machine.
 *
 * Tests verify all state transitions according to the state diagram:
 * - CLOSED <-> OPENING <-> OPEN <-> CLOSING <-> CLOSED
 * - OPENING <-> PAUSED_OPENING
 * - CLOSING <-> PAUSED_CLOSING
 * - Obstacle reversal: OPENING -> CLOSING, CLOSING -> OPENING
 */
class DoorControllerTest {

    private DoorController door;

    @BeforeEach
    void setUp() {
        door = new DoorController();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("Door starts in CLOSED state")
        void doorStartsClosed() {
            assertEquals(DoorState.CLOSED, door.getCurrentState());
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
            assertEquals(DoorState.OPENING, door.getCurrentState());
            assertTrue(door.isMoving());
        }

        @Test
        @DisplayName("OPENING + P -> PAUSED_OPENING")
        void openingToPausedOpening() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSED -> OPENING
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPENING -> PAUSED_OPENING
            assertEquals(DoorState.PAUSED_OPENING, door.getCurrentState());
            assertTrue(door.isPaused());
        }

        @Test
        @DisplayName("PAUSED_OPENING + P -> OPENING (resume)")
        void pausedOpeningToOpening() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSED -> OPENING
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPENING -> PAUSED_OPENING
            door.processEvent(DoorEvent.BUTTON_PRESS); // PAUSED_OPENING -> OPENING
            assertEquals(DoorState.OPENING, door.getCurrentState());
        }

        @Test
        @DisplayName("OPEN + P -> CLOSING")
        void openToClosing() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // Start opening
            door.advanceTime(5);                        // Complete opening
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPEN -> CLOSING
            assertEquals(DoorState.CLOSING, door.getCurrentState());
        }

        @Test
        @DisplayName("CLOSING + P -> PAUSED_CLOSING")
        void closingToPausedClosing() {
            // Get to OPEN state
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);

            // Start closing then pause
            door.processEvent(DoorEvent.BUTTON_PRESS); // OPEN -> CLOSING
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSING -> PAUSED_CLOSING
            assertEquals(DoorState.PAUSED_CLOSING, door.getCurrentState());
        }

        @Test
        @DisplayName("PAUSED_CLOSING + P -> CLOSING (resume)")
        void pausedClosingToClosing() {
            // Get to PAUSED_CLOSING
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.BUTTON_PRESS);

            // Resume
            door.processEvent(DoorEvent.BUTTON_PRESS);
            assertEquals(DoorState.CLOSING, door.getCurrentState());
        }
    }

    @Nested
    @DisplayName("Obstacle Detection Transitions")
    class ObstacleTransitions {

        @Test
        @DisplayName("OPENING + O -> CLOSING (reverse)")
        void openingToClosingOnObstacle() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // CLOSED -> OPENING
            door.processEvent(DoorEvent.OBSTACLE);     // OPENING -> CLOSING
            assertEquals(DoorState.CLOSING, door.getCurrentState());
        }

        @Test
        @DisplayName("CLOSING + O -> OPENING (reverse, safety feature)")
        void closingToOpeningOnObstacle() {
            // Get to CLOSING state
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);

            // Obstacle while closing triggers safety reversal
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals(DoorState.OPENING, door.getCurrentState());
        }

        @Test
        @DisplayName("Obstacle has no effect when CLOSED")
        void obstacleIgnoredWhenClosed() {
            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals(DoorState.CLOSED, door.getCurrentState());
        }

        @Test
        @DisplayName("Obstacle has no effect when OPEN")
        void obstacleIgnoredWhenOpen() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);

            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals(DoorState.OPEN, door.getCurrentState());
        }

        @Test
        @DisplayName("Obstacle has no effect when PAUSED")
        void obstacleIgnoredWhenPaused() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.processEvent(DoorEvent.BUTTON_PRESS); // PAUSED_OPENING

            door.processEvent(DoorEvent.OBSTACLE);
            assertEquals(DoorState.PAUSED_OPENING, door.getCurrentState());
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
            assertEquals(DoorState.OPEN, door.getCurrentState());
        }

        @Test
        @DisplayName("CLOSING completes to CLOSED after 5 seconds")
        void closingCompletesToClosed() {
            // Get to CLOSING
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            door.processEvent(DoorEvent.BUTTON_PRESS);

            boolean completed = door.advanceTime(5);
            assertTrue(completed);
            assertEquals(DoorState.CLOSED, door.getCurrentState());
        }

        @Test
        @DisplayName("Partial movement tracked correctly")
        void partialMovementTracked() {
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(2);

            assertEquals(2, door.getElapsedTime());
            assertEquals(3, door.getRemainingTime());
            assertEquals(DoorState.OPENING, door.getCurrentState());
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
            assertEquals(DoorState.PAUSED_OPENING, door.getCurrentState());
        }
    }

    @Nested
    @DisplayName("Full Cycle Tests")
    class FullCycleTests {

        @Test
        @DisplayName("Complete open-close cycle")
        void completeOpenCloseCycle() {
            // Open
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            assertEquals(DoorState.OPEN, door.getCurrentState());

            // Close
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);
            assertEquals(DoorState.CLOSED, door.getCurrentState());
        }

        @Test
        @DisplayName("Pause, resume, and complete")
        void pauseResumeComplete() {
            door.processEvent(DoorEvent.BUTTON_PRESS); // Start opening
            door.advanceTime(2);                        // 2 seconds elapsed
            door.processEvent(DoorEvent.BUTTON_PRESS); // Pause
            door.processEvent(DoorEvent.BUTTON_PRESS); // Resume
            door.advanceTime(3);                        // Complete remaining 3 seconds

            assertEquals(DoorState.OPEN, door.getCurrentState());
        }

        @Test
        @DisplayName("Obstacle reversal during closing completes to OPEN")
        void obstacleReversalCompletesToOpen() {
            // Open fully
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(5);

            // Start closing, then obstacle
            door.processEvent(DoorEvent.BUTTON_PRESS);
            door.advanceTime(2); // 2 seconds into closing
            door.processEvent(DoorEvent.OBSTACLE); // Reverse!

            // Should take ~2 seconds to get back to open
            door.advanceTime(2);
            assertEquals(DoorState.OPEN, door.getCurrentState());
        }
    }
}
