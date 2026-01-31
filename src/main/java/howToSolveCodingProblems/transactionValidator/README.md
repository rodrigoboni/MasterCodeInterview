# Transaction Validator - Java Concurrency Challenge

A thread-safe financial transaction validation system demonstrating essential Java concurrency concepts for coding interviews.

## Table of Contents

- [Overview](#overview)
- [The Challenge](#the-challenge)
- [Architecture](#architecture)
- [Concurrency Concepts](#concurrency-concepts)
  - [1. AtomicReference & CAS](#1-atomicreference--cas-compare-and-swap)
  - [2. ReentrantLock](#2-reentrantlock-vs-synchronized)
  - [3. ConcurrentHashMap](#3-concurrenthashmap-for-duplicate-detection)
- [Design Patterns](#design-patterns)
  - [Result Pattern](#result-pattern-sealed-interfaces)
  - [Immutable Objects](#immutable-objects-java-records)
  - [Fail-Fast Principle](#fail-fast-principle)
  - [Idempotency](#idempotency)
- [Java 17+ Features](#java-17-features)
- [Testing Concurrency](#testing-concurrency)
- [Complexity Analysis](#complexity-analysis)
- [Running the Code](#running-the-code)

---

## Overview

This implementation demonstrates how to build a **thread-safe transaction validation system** that:

1. ✅ **Checks balance** before withdrawals
2. ✅ **Prevents duplicate transactions** using atomic operations
3. ✅ **Handles concurrent requests** safely without race conditions

## The Challenge

> Build a system to validate financial transactions: check balance, prevent duplicate transactions, handle concurrent requests safely.

This tests your understanding of:
- Race conditions and how to prevent them
- Lock-based vs lock-free synchronization
- Atomic operations and CAS loops
- Testing concurrent code

---

## Architecture

```
┌──────────────────┐     ┌─────────────────────┐     ┌─────────────────────────┐
│   Transaction    │────▶│ TransactionValidator │────▶│   ValidationResult     │
│   (Record)       │     │    (Main Service)    │     │   (Sealed Interface)   │
└──────────────────┘     └─────────────────────┘     └─────────────────────────┘
                                   │
                                   ▼
                         ┌─────────────────┐
                         │     Account     │
                         │ (Thread-Safe)   │
                         └─────────────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           ▼                       ▼                       ▼
  ┌────────────────┐    ┌─────────────────┐    ┌─────────────────────┐
  │ AtomicReference│    │ConcurrentHashMap│    │   ReentrantLock     │
  │   (Balance)    │    │ (Processed IDs) │    │ (Compound Actions)  │
  └────────────────┘    └─────────────────┘    └─────────────────────┘
```

### File Structure

```
transactionValidator/
├── model/
│   ├── TransactionType.java      # Enum: DEPOSIT, WITHDRAWAL, TRANSFER
│   ├── Transaction.java          # Immutable record with validation
│   └── Account.java              # Thread-safe account with ReentrantLock
├── validation/
│   ├── ValidationResult.java     # Sealed interface
│   ├── Success.java              # Successful validation result
│   ├── InsufficientFunds.java    # Failed: not enough balance
│   ├── DuplicateTransaction.java # Failed: already processed
│   └── InvalidInput.java         # Failed: invalid input data
├── TransactionValidator.java     # Main service class
├── TransactionValidatorDemo.java # Interactive demo
└── README.md                     # This file
```

---

## Concurrency Concepts

### 1. AtomicReference & CAS (Compare-And-Swap)

**Used for:** Deposits (balance updates without locking)

#### The Problem
Multiple threads updating balance simultaneously can lose updates:

```java
// BROKEN - Race condition!
balance = balance + amount;  // Read-modify-write is NOT atomic
```

#### The Solution: CAS Loop

```java
// Thread-safe with AtomicReference
balance.updateAndGet(current -> current.add(amount));
```

**How CAS works:**

```
Time   Thread A                    Thread B
────   ────────────────────────    ────────────────────────
T0     Read balance = $100
T1     Compute $100 + $50 = $150
T1                                 Read balance = $100
T2                                 Compute $100 + $30 = $130
T3     CAS($100 → $150) ✓ SUCCESS
T4                                 CAS($100 → $130) ✗ FAIL! (balance is now $150)
T5                                 RETRY: Read $150, compute $180
T6                                 CAS($150 → $180) ✓ SUCCESS

Final: $180 ✓ (correct: $100 + $50 + $30)
```

#### Why AtomicReference<BigDecimal> instead of AtomicLong?

| Approach | Pros | Cons |
|----------|------|------|
| `AtomicLong` (cents) | Faster, no object allocation | Limited precision, overflow risk |
| `AtomicReference<BigDecimal>` | Arbitrary precision, natural representation | Object allocation on update |

**For financial systems, precision trumps micro-optimization.**

---

### 2. ReentrantLock vs synchronized

**Used for:** Withdrawals (check-then-act atomicity)

#### The Problem: Check-Then-Act Race Condition

```java
// BROKEN - Race condition!
if (balance >= amount) {     // Thread A: $100 >= $80 ✓
                             // Thread B: $100 >= $50 ✓ (checks before A updates!)
    balance -= amount;       // Thread A: $100 - $80 = $20
                             // Thread B: $20 - $50 = -$30  ❌ NEGATIVE!
}
```

#### The Solution: ReentrantLock

```java
withdrawalLock.lock();
try {
    if (balance.get().compareTo(amount) >= 0) {
        balance.set(current.subtract(amount));
        return true;
    }
    return false;
} finally {
    withdrawalLock.unlock();  // ALWAYS in finally!
}
```

#### Why ReentrantLock over synchronized?

| Feature | synchronized | ReentrantLock |
|---------|-------------|---------------|
| Timeout on acquire | ❌ No | ✅ `tryLock(time, unit)` |
| Interruptible wait | ❌ No | ✅ `lockInterruptibly()` |
| Fair ordering (FIFO) | ❌ No | ✅ `new ReentrantLock(true)` |
| Multiple conditions | ❌ No | ✅ `newCondition()` |
| Check if held | ❌ No | ✅ `isLocked()`, `isHeldByCurrentThread()` |
| Automatic unlock | ✅ Yes | ❌ Manual in finally |

**We use ReentrantLock with fair=true to prevent thread starvation.**

---

### 3. ConcurrentHashMap for Duplicate Detection

**Used for:** Preventing duplicate transaction processing

#### The Key Insight: `putIfAbsent()` is ATOMIC

```java
// Single atomic operation - check AND add
Instant previous = processedTransactions.putIfAbsent(transactionId, Instant.now());
boolean isNewTransaction = (previous == null);
```

#### How it prevents duplicates:

```
Thread A: putIfAbsent("TXN-001", now) → returns null (added!)
Thread B: putIfAbsent("TXN-001", now) → returns Instant (already exists!)

Even if both threads call simultaneously, only ONE succeeds in adding.
```

#### Why store `Instant` instead of `Boolean`?

1. **Audit trail** - When was it first processed?
2. **Debugging** - Track transaction timing
3. **Cleanup** - Could implement TTL-based eviction

---

## Design Patterns

### Result Pattern (Sealed Interfaces)

Instead of throwing exceptions for expected failures, we return typed results:

```java
public sealed interface ValidationResult
    permits Success, InsufficientFunds, DuplicateTransaction, InvalidInput {

    boolean isSuccess();
    String message();
}
```

#### Result Pattern vs Exceptions

| Aspect | Exceptions | Result Pattern |
|--------|------------|----------------|
| Type signature | Hidden (throws) | Explicit in return type |
| Performance | Stack trace overhead | No overhead |
| Handling | Can be ignored | Must handle all cases |
| Best for | Unexpected errors | Expected failures |

#### Pattern Matching (Java 17+)

```java
if (result instanceof Success s) {
    System.out.println("New balance: " + s.newBalance());
} else if (result instanceof InsufficientFunds f) {
    System.out.println("Need $" + f.shortfall() + " more");
} else if (result instanceof DuplicateTransaction d) {
    System.out.println("Already processed: " + d.transactionId());
} else if (result instanceof InvalidInput i) {
    System.out.println("Error: " + i.reason());
}
```

---

### Immutable Objects (Java Records)

```java
public record Transaction(
    String transactionId,
    String accountId,
    TransactionType type,
    BigDecimal amount,
    Instant timestamp
) {
    // Compact constructor for validation
    public Transaction {
        Objects.requireNonNull(transactionId);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
```

#### Why Immutability Matters for Concurrency

1. **Thread-safe by design** - No synchronization needed
2. **No race conditions** - State cannot change after construction
3. **Safe publication** - Can be freely shared between threads
4. **Failure atomicity** - Object is either fully valid or not created

---

### Fail-Fast Principle

Detect and report errors as early as possible:

```java
// Step 1: Null check (fail immediately)
if (transaction == null) {
    return new InvalidInput("Transaction cannot be null");
}

// Step 2: Account lookup (fail before business logic)
Account account = accounts.get(transaction.accountId());
if (account == null) {
    return new InvalidInput("Account not found");
}

// Step 3: Duplicate check (fail before expensive operations)
if (!account.markProcessed(transaction.transactionId())) {
    return new DuplicateTransaction(transaction.transactionId());
}

// Step 4: Now safe to execute
```

---

### Idempotency

An operation is **idempotent** if performing it multiple times has the same effect as performing it once.

#### Why It Matters

In distributed systems:
1. Client sends request
2. Network timeout (response lost)
3. Client retries the same request
4. Server receives it twice!

Without idempotency:
- User charged twice
- Money transferred twice
- Inventory decremented twice

#### Our Implementation

```java
// Client generates unique ID before sending
String transactionId = UUID.randomUUID().toString();

// Server tracks all processed IDs
if (!account.markProcessed(transactionId)) {
    return new DuplicateTransaction(transactionId);
}
// Only ONE thread ever processes a given ID
```

---

## Java 17+ Features

### 1. Records (Java 16+)

```java
// Before: 50+ lines of boilerplate
public class Transaction {
    private final String id;
    private final BigDecimal amount;
    // constructor, getters, equals, hashCode, toString...
}

// After: 5 lines
public record Transaction(String id, BigDecimal amount) { }
```

### 2. Sealed Interfaces (Java 17)

```java
// Compiler knows ALL possible implementations
public sealed interface ValidationResult
    permits Success, InsufficientFunds, DuplicateTransaction, InvalidInput
```

### 3. Pattern Matching for instanceof (Java 17)

```java
// Before
if (result instanceof Success) {
    Success s = (Success) result;
    return s.newBalance();
}

// After
if (result instanceof Success s) {
    return s.newBalance();
}
```

---

## Testing Concurrency

### The Challenge

- Race conditions are non-deterministic
- Bugs may only appear under specific timing
- Thread scheduling varies between runs

### Pattern: CountDownLatch (Starting Gun)

```java
CountDownLatch startLatch = new CountDownLatch(1);  // The "starting gun"
CountDownLatch doneLatch = new CountDownLatch(numThreads);  // Finish line

// Submit tasks
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        startLatch.await();  // All threads wait here
        // ... do work ...
        doneLatch.countDown();
    });
}

startLatch.countDown();  // Fire! All threads start simultaneously
doneLatch.await();       // Wait for all to finish
```

### Test Examples

| Test | Scenario | Assertion |
|------|----------|-----------|
| Concurrent Withdrawals | 100 threads × $50 from $1000 | Exactly 20 succeed |
| Concurrent Duplicates | 100 threads × same ID | Exactly 1 succeeds |
| Concurrent Deposits | 1000 threads × $1 | Balance = $1000 |

---

## Complexity Analysis

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| `process()` | O(1) | O(1) | Amortized for CAS retries |
| `deposit()` | O(1) | O(1) | CAS loop, may retry |
| `withdraw()` | O(1) | O(1) | Blocks on lock contention |
| `markProcessed()` | O(1) avg | O(1) | ConcurrentHashMap |
| **Total storage** | - | O(a + t) | a=accounts, t=transactions |

---

## Running the Code

### Run Tests

```bash
mvn test -Dtest=TransactionValidatorTest
```

### Run Demo

```bash
mvn compile && java -cp target/classes \
  howToSolveCodingProblems.transactionValidator.TransactionValidatorDemo
```

### Expected Demo Output

```
══════════════════════════════════════════════════════════════════════
      TRANSACTION VALIDATOR DEMO
══════════════════════════════════════════════════════════════════════

✓ Deposit $500         → Transaction successful. New balance: 1500.00
✓ Withdraw $200        → Transaction successful. New balance: 1300.00
✗ Duplicate TXN-001    → Duplicate transaction rejected. ID: TXN-001
✗ Withdraw $5000       → Insufficient funds. Shortfall: 3700.00

Concurrent withdrawals: 20 succeed from $1000, 30 fail ✓
Concurrent duplicates:  1 succeeds, 19 detected as duplicates ✓
```

---

## Interview Tips

1. **Always mention BigDecimal for money** - Using `double` is a red flag
2. **Discuss idempotency** when designing APIs that modify state
3. **Know when to use locks vs CAS** - Locks for check-then-act, CAS for simple updates
4. **Explain the Result Pattern** - Shows understanding of functional programming concepts
5. **Demonstrate testing strategies** - CountDownLatch for concurrent tests

---

## Further Reading

- [Java Concurrency in Practice](https://jcip.net/) - The definitive book
- [Java AtomicReference Documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/atomic/AtomicReference.html)
- [ReentrantLock vs synchronized](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html)
- [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)
