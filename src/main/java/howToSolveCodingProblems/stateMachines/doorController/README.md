# Door Controller State Machine Challenge

## Problem Statement

**Event Handler State Machine** - Implement a door controller that processes events:
- **Button Press (P)**: Toggle door movement or pause
- **Obstacle Detection (O)**: Safety reversal during movement

**Constraints:**
- Door takes **5 seconds** to fully open/close
- Can **pause/resume** at any point
- **Reverses** direction when obstacle detected during movement

---

## State Diagram

```
                         ┌─────────────────────────────────────────────┐
                         │                                             │
                         ▼                                             │
                    ┌─────────┐                                   ┌─────────┐
             ┌──────│ CLOSED  │◄────────── COMPLETE ──────────────│ CLOSING │◄────┐
             │      └─────────┘                                   └─────────┘     │
             │           │                                             │  ▲       │
             P           │                                             P  │       O
             │           │                                             │  │       │
             ▼           │                                             ▼  │       │
        ┌─────────┐      │                                      ┌──────────────┐  │
        │ OPENING │──────┼─────────────── O ────────────────────│PAUSED_CLOSING│  │
        └─────────┘      │                                      └──────────────┘  │
             │  ▲        │                                                        │
             P  │        │                                                        │
             │  │        │                                                        │
             ▼  │        │                                                        │
     ┌──────────────┐    │                                                        │
     │PAUSED_OPENING│    │                                                        │
     └──────────────┘    │                                                        │
             │           │                                                        │
             │      ┌─────────┐                                                   │
             └──────│  OPEN   │◄───────────── COMPLETE ───────────────────────────┘
                    └─────────┘
                         │
                         P
                         │
                         ▼
                    (CLOSING)
```

### State Transition Table

| Current State    | Button Press (P) | Obstacle (O) | Complete (5s) |
|------------------|------------------|--------------|---------------|
| **CLOSED**       | → OPENING        | (no change)  | (no change)   |
| **OPENING**      | → PAUSED_OPENING | → CLOSING    | → OPEN        |
| **OPEN**         | → CLOSING        | (no change)  | (no change)   |
| **CLOSING**      | → PAUSED_CLOSING | → OPENING    | → CLOSED      |
| **PAUSED_OPENING** | → OPENING      | (no change)  | (no change)   |
| **PAUSED_CLOSING** | → CLOSING      | (no change)  | (no change)   |

---

## Two Implementation Approaches

This package contains **two implementations** of the same state machine:

### 1. Switch-Based Implementation (V1)
**Location:** `DoorController.java`

Traditional approach using:
- Enum for states (`DoorState.java`)
- Switch statements for state transitions
- Handler methods per state

### 2. State Pattern Implementation (V2)
**Location:** `statepattern/` subpackage

Object-oriented approach using:
- Interface for state behavior (`DoorStateHandler`)
- Concrete class per state (6 classes)
- Polymorphic dispatch (no switch on state)

---

## Design Patterns Explained

### 1. State Pattern (GoF)

> **Intent:** Allow an object to alter its behavior when its internal state changes. The object will appear to change its class.

**How it's used:**
```java
// Interface defines what each state can do
public interface DoorStateHandler {
    DoorStateHandler onButtonPress(DoorContext context);
    DoorStateHandler onObstacle(DoorContext context);
    DoorStateHandler onComplete(DoorContext context);
}

// Each state is a class
public class ClosedState implements DoorStateHandler {
    @Override
    public DoorStateHandler onButtonPress(DoorContext context) {
        return OpeningState.INSTANCE;  // Transition defined HERE
    }
}
```

**Benefits:**
- Each state's behavior is encapsulated in its own class
- Adding new states doesn't modify existing code (Open/Closed Principle)
- State-specific logic is colocated with state identity

---

### 2. Template Method Pattern (GoF)

> **Intent:** Define the skeleton of an algorithm in a base class, letting subclasses override specific steps.

**How it's used:**
```java
public abstract class AbstractMovingState {

    // Template method - defines the algorithm
    public boolean advanceTime(DoorContext context, int seconds) {
        MovementData movement = context.getMovementData();
        movement.advanceTime(seconds);
        if (movement.isComplete()) {
            context.processEvent(DoorEvent.COMPLETE);
            return true;
        }
        return false;
    }

    // Abstract methods - subclasses provide specific states
    protected abstract DoorStateHandler getPausedState();
    protected abstract DoorStateHandler getReverseState();
    protected abstract DoorStateHandler getCompletedState();
}
```

**Benefits:**
- Time advancement logic written once
- Subclasses only define their specific transitions
- Consistent behavior across all moving states

---

### 3. Singleton Pattern (GoF)

> **Intent:** Ensure a class has only one instance and provide a global point of access to it.

**How it's used:**
```java
public class ClosedState extends AbstractDoorState {
    // Single instance - safe because state is STATELESS
    public static final ClosedState INSTANCE = new ClosedState();

    private ClosedState() {
        super("CLOSED");
    }
}
```

**Benefits:**
- Memory efficient (one object per state type)
- Safe because states don't hold mutable data
- Easy to compare states with `==`

---

### 4. Strategy Pattern (GoF)

> **Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

**How it's used:**
```java
public class MovementData {
    private int elapsedTime;
    private int remainingTime;

    public void advanceTime(int seconds) { ... }
    public void reverse() { ... }  // Algorithm for direction change
    public boolean isComplete() { ... }
}
```

**Benefits:**
- Time tracking algorithm separate from state logic
- Could swap for different timing implementations
- Testable in isolation

---

## Code Comparison: Before vs After

### V1: Switch-Based (DoorController.java)

```java
public DoorState processEvent(DoorEvent event) {
    switch (currentState) {
        case CLOSED:
            handleClosedState(event);    // Handler method
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
    return currentState;
}

private void handleClosedState(DoorEvent event) {
    if (event == DoorEvent.BUTTON_PRESS) {
        currentState = DoorState.OPENING;
        elapsedTime = 0;
        remainingTime = FULL_MOVEMENT_TIME;
    }
}
```

### V2: State Pattern (DoorControllerV2.java)

```java
public void processEvent(DoorEvent event) {
    DoorStateHandler previousState = currentState;

    // NO SWITCH ON STATE - pure polymorphic dispatch!
    DoorStateHandler nextState = switch (event) {
        case BUTTON_PRESS -> currentState.onButtonPress(this);
        case OBSTACLE -> currentState.onObstacle(this);
        case COMPLETE -> currentState.onComplete(this);
    };

    if (nextState != previousState) {
        previousState.onExit(this);
        currentState = nextState;
        currentState.onEnter(this);
    }
}
```

---

## Comparison Summary

| Criteria | V1 (Switch-Based) | V2 (State Pattern) |
|----------|-------------------|---------------------|
| **Lines of code** | ~210 (1 file) | ~400 (13 files) |
| **Initial complexity** | Lower | Higher |
| **Maintainability** | Decreases with states | Stays constant |
| **Adding new state** | Modify 3+ places | Create 1 new class |
| **Adding new event** | Modify 6 handlers | Add 1 method + implement |
| **Testability** | Integration only | Unit + Integration |
| **Learning curve** | Easier | Requires pattern knowledge |
| **Open/Closed Principle** | Violates | Follows |

---

## When to Use Each Approach

### Use V1 (Switch-Based) When:
- ✅ Small number of states (≤ 5)
- ✅ States unlikely to change
- ✅ Team unfamiliar with State Pattern
- ✅ Simple state logic
- ✅ Prototyping or quick implementation

### Use V2 (State Pattern) When:
- ✅ Many states or growing state count
- ✅ Complex state-specific behavior
- ✅ Need to test states in isolation
- ✅ Following SOLID principles
- ✅ Building production/enterprise code

---

## Running the Code

### Run V1 Demo (Switch-Based)
```bash
mvn compile && java -cp target/classes \
  howToSolveCodingProblems.stateMachines.doorController.DoorControllerDemo
```

### Run V2 Demo (State Pattern)
```bash
mvn compile && java -cp target/classes \
  howToSolveCodingProblems.stateMachines.doorController.statepattern.DoorControllerV2Demo
```

### Run All Tests
```bash
mvn test -Dtest="**/doorController/**Test"
```

### Run V1 Tests Only
```bash
mvn test -Dtest="**/doorController/DoorControllerTest"
```

### Run V2 Tests Only
```bash
mvn test -Dtest="**/statepattern/*Test"
```

---

## File Structure

```
doorController/
├── DoorState.java              # Enum: CLOSED, OPENING, OPEN, etc.
├── DoorEvent.java              # Enum: BUTTON_PRESS, OBSTACLE, COMPLETE
├── DoorController.java         # V1: Switch-based implementation
├── DoorControllerDemo.java     # V1: Demo scenarios
├── README.md                   # This documentation
│
└── statepattern/               # V2: State Pattern implementation
    ├── DoorStateHandler.java       # Interface for states
    ├── AbstractDoorState.java      # Base class (default behavior)
    ├── AbstractMovingState.java    # Template for OPENING/CLOSING
    ├── ClosedState.java            # Concrete state
    ├── OpeningState.java           # Concrete state
    ├── OpenState.java              # Concrete state
    ├── ClosingState.java           # Concrete state
    ├── PausedOpeningState.java     # Concrete state
    ├── PausedClosingState.java     # Concrete state
    ├── DoorContext.java            # Context interface
    ├── MovementData.java           # Time tracking (Strategy)
    ├── DoorControllerV2.java       # Main controller
    └── DoorControllerV2Demo.java   # Demo scenarios
```

---

## Key Takeaways

1. **State machines are everywhere**: Elevators, traffic lights, vending machines, garage doors, TCP connections

2. **Switch statements work fine** for simple cases but become maintenance nightmares as states grow

3. **State Pattern** trades initial complexity for long-term maintainability

4. **Design patterns complement each other**: State + Template Method + Singleton + Strategy all work together

5. **Both approaches produce identical behavior** - the difference is in code organization and extensibility

---

## References

- [State Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/state)
- [Template Method Pattern](https://refactoring.guru/design-patterns/template-method)
- [Design Patterns: Elements of Reusable Object-Oriented Software](https://en.wikipedia.org/wiki/Design_Patterns) (GoF Book)
