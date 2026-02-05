# Top Java Concurrency Interview Questions

A comprehensive Q&A guide targeting **senior software engineers**, covering the Java Memory Model, threads, synchronization, locks, atomic classes, concurrent collections, and modern virtual threads.

**Java version:** Examples target **Java 17+**. Sections marked *(Java 21+)* require preview or GA features from that release.

> **Companion code:** The [TransactionValidator](src/main/java/howToSolveCodingProblems/transactionValidator/README.md) project in this repo demonstrates many of these concepts with production-style code. Cross-references appear throughout this document.

---

## Table of Contents

1. [Java Memory Model (JMM)](#1-java-memory-model-jmm)
2. [Threads & Thread Creation](#2-threads--thread-creation)
3. [Synchronization](#3-synchronization)
4. [Volatile Keyword](#4-volatile-keyword)
5. [Stopping Threads Safely](#5-stopping-threads-safely)
6. [Joining Threads](#6-joining-threads)
7. [Executor Framework & Queues](#7-executor-framework--queues)
8. [CompletableFuture & Advanced Async](#8-completablefuture--advanced-async)
9. [Lock API & Synchronization Utilities](#9-lock-api--synchronization-utilities)
10. [Atomic Classes & Lock-Free Programming](#10-atomic-classes--lock-free-programming)
11. [Concurrent Collections & ThreadLocal](#11-concurrent-collections--threadlocal)
12. [Virtual Threads & Modern Concurrency (Java 21+)](#12-virtual-threads--modern-concurrency-java-21)

---

## 1. Java Memory Model (JMM)

### **Q1: What is the Java Memory Model and why does it exist?**

The **Java Memory Model (JMM)**, defined by **JSR-133** (revised in Java 5), is the specification that describes how threads interact through memory. It exists because:

1. **Hardware diversity** — CPUs have store buffers, caches (L1/L2/L3), and write-combining buffers. Without a memory model, program behavior would vary across Intel x86, ARM, and other architectures.
2. **Compiler optimizations** — The JIT compiler reorders instructions, eliminates dead stores, and hoists reads out of loops. The JMM defines which reorderings are legal.
3. **Portability** — Java's "write once, run anywhere" promise requires a formal contract between the programmer and the JVM.

The JMM answers one question: **when is a write by thread A guaranteed to be visible to a read by thread B?** The answer is formalized through the *happens-before* relationship.

---

### **Q2: What is the happens-before relationship?**

**Happens-before** is a partial ordering on actions in a Java program. If action A *happens-before* action B, then:

- The effects of A are **guaranteed visible** to B.
- A is ordered **before** B (no reordering across the edge).

It does **not** mean A occurs first in wall-clock time — it means B can *see* A's effects.

#### Complete Happens-Before Rules

| # | Rule | Description |
|---|------|-------------|
| 1 | **Program order** | Each action in a thread happens-before every subsequent action in that same thread. |
| 2 | **Monitor lock** | An `unlock` on a monitor happens-before every subsequent `lock` on that same monitor. |
| 3 | **Volatile variable** | A write to a `volatile` field happens-before every subsequent read of that field. |
| 4 | **Thread start** | A call to `Thread.start()` happens-before any action in the started thread. |
| 5 | **Thread join** | All actions in a thread happen-before another thread successfully returns from `join()` on that thread. |
| 6 | **Thread interrupt** | A call to `Thread.interrupt()` happens-before the interrupted thread detects the interrupt. |
| 7 | **Finalizer** | The end of a constructor happens-before the start of the finalizer for that object. |
| 8 | **Transitivity** | If A happens-before B, and B happens-before C, then A happens-before C. |

---

### **Q3: What does "synchronizes-with" mean?**

**Synchronizes-with** is the *source* of happens-before edges between threads. It is an inter-thread relationship:

- An **unlock** of monitor M *synchronizes-with* the subsequent **lock** of M.
- A **write** to a volatile variable V *synchronizes-with* the subsequent **read** of V.
- The action that starts a thread *synchronizes-with* the first action of the new thread.
- The final action of a thread *synchronizes-with* any action that detects termination (e.g. `join()`).

The JMM combines *synchronizes-with* edges with *program-order* edges (and transitivity) to form the complete happens-before graph.

---

### **Q4: What is a memory visibility problem? Show an example.**

A visibility problem occurs when one thread's writes are not seen by another thread because there is no happens-before relationship.

```java
// BROKEN — visibility bug
public class StaleValueDemo {
    private boolean running = true;  // No volatile!

    public void runLoop() {
        while (running) {   // May read stale cached value forever
            // do work
        }
        System.out.println("Stopped");  // May never print!
    }

    public void stop() {
        running = false;  // Write may never become visible to runLoop()
    }
}
```

**Why it breaks:** Without `volatile` or synchronization, the JIT compiler can hoist the read of `running` out of the loop, turning it into:

```java
if (running) {
    while (true) { /* do work */ }  // Infinite loop!
}
```

**Fix:** Declare `running` as `volatile` (see [Section 4](#4-volatile-keyword)).

---

### **Q5: Can the JVM reorder instructions? What constraints does the JMM impose?**

Yes. Both the **compiler** (JIT) and the **CPU** may reorder instructions, as long as the result is consistent with **sequential consistency within a single thread** (the *as-if-serial* guarantee).

However, the JMM constrains reordering across happens-before edges:

- No reordering of a volatile write with any preceding action in program order.
- No reordering of a volatile read with any subsequent action in program order.
- No moving instructions out of a `synchronized` block.

This is why the broken double-checked locking idiom was fixed in Java 5: the `volatile` write of the singleton reference prevents reordering the constructor's stores with the reference assignment.

---

### **Q6: Explain the transitivity (piggyback) pattern.**

Transitivity means: if A happens-before B, and B happens-before C, then A happens-before C. This allows you to **piggyback** on an existing happens-before edge.

```java
// Thread 1
x = 42;                // (a)
volatileFlag = true;   // (b) volatile write — happens-before (c)

// Thread 2
if (volatileFlag) {    // (c) volatile read
    // x is guaranteed to be 42 here!
    // Because (a) hb (b) [program order], (b) hb (c) [volatile], ∴ (a) hb (c)
}
```

This is the **publication flag** pattern — a single volatile write publishes all writes made before it. The same principle underlies `CountDownLatch.countDown()` and other `java.util.concurrent` classes.

---

### **Q7: What happens-before guarantee does `Thread.join()` provide?**

When thread A calls `threadB.join()` and it returns successfully, **all actions performed by thread B** happen-before the return of `join()` in thread A. This means thread A can see every write that thread B made, without any additional synchronization.

```java
Thread worker = new Thread(() -> {
    // All stores here...
    sharedData = computeResult();
});
worker.start();
worker.join();  // Happens-before edge: worker's writes → this read
System.out.println(sharedData);  // Guaranteed to see the computed result
```

See also [Section 6](#6-joining-threads) for `join()` overloads and alternatives.

---

## 2. Threads & Thread Creation

### **Q8: What are the different ways to create threads in Java?**

There are **five** principal approaches:

#### 1. Extend `Thread`

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Running in " + getName());
    }
}
new MyThread().start();
```

#### 2. Implement `Runnable`

```java
Runnable task = () -> System.out.println("Running in " + Thread.currentThread().getName());
new Thread(task).start();
```

#### 3. Implement `Callable<V>` (with `Future`)

```java
Callable<Integer> task = () -> {
    TimeUnit.SECONDS.sleep(1);
    return 42;
};
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(task);
System.out.println("Result: " + future.get());  // Blocks until done
executor.shutdown();
```

#### 4. `ExecutorService` (preferred)

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> System.out.println("Task on pool thread"));
executor.shutdown();
```

#### 5. `CompletableFuture`

```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> transform(data))
    .thenAccept(result -> save(result));
```

---

### **Q9: Why is implementing `Runnable` preferred over extending `Thread`?**

| Aspect | `extends Thread` | `implements Runnable` |
|--------|-------------------|-----------------------|
| Inheritance | Consumes your single superclass | Interface — can still extend another class |
| Separation of concerns | Task and execution coupled | Task logic separated from thread management |
| Reusability | One thread per instance | Same Runnable reusable across thread pools |
| Testability | Hard to unit-test | Easy to test `run()` directly |
| Executor compatibility | Must wrap in Runnable anyway | Works directly with ExecutorService |

**Rule of thumb:** Only extend `Thread` if you need to override thread behavior (almost never).

---

### **Q10: Compare `Runnable` vs `Callable`.**

| Feature | `Runnable` | `Callable<V>` |
|---------|------------|----------------|
| Return value | `void` | `V` (generic) |
| Checked exceptions | Cannot throw | Can throw `Exception` |
| Functional interface | `run()` | `call()` |
| Use with Future | No direct support | `executor.submit(callable)` → `Future<V>` |
| Lambda compatible | Yes | Yes |

Use `Callable` when you need a result or need to propagate checked exceptions.

---

### **Q11: What are the benefits of the Executor Framework over raw threads?**

1. **Thread reuse** — Pool threads are recycled, avoiding the cost of `new Thread()` (~1 MB stack allocation + OS kernel call).
2. **Bounded concurrency** — A fixed pool caps the number of live threads, preventing resource exhaustion.
3. **Task/thread decoupling** — You submit tasks; the framework manages threads.
4. **Lifecycle management** — `shutdown()`, `awaitTermination()`, `shutdownNow()` provide orderly cleanup.
5. **Scheduling** — `ScheduledExecutorService` supports delayed and periodic tasks.
6. **Return values** — `submit()` returns a `Future<V>` for result retrieval.

---

### **Q12: How do parallel streams work internally, and what are the risks?**

Parallel streams use the **ForkJoinPool.commonPool()** under the hood:

```java
list.parallelStream()
    .filter(x -> x > 10)
    .map(x -> transform(x))
    .collect(Collectors.toList());
```

**Risks:**

1. **Shared common pool** — All parallel streams in the JVM share one pool. A blocking operation in one stream starves others.
2. **Non-deterministic ordering** — Results may arrive in any order unless you use `forEachOrdered()`.
3. **Thread-unsafe collectors** — Using a non-thread-safe accumulator causes data corruption.
4. **Overhead** — For small datasets, the fork/join overhead exceeds the benefit of parallelism.
5. **Side effects** — Parallel streams should not modify shared mutable state.

**Guideline:** Use parallel streams only for CPU-bound work on large datasets (> 10,000 elements as a rough threshold), and never with I/O-bound operations.

---

### **Q13: Explain the Fork/Join Framework.**

Fork/Join is a work-stealing framework (since Java 7) designed for **recursive divide-and-conquer** parallelism.

```java
public class SumTask extends RecursiveTask<Long> {
    private final long[] array;
    private final int start, end;
    private static final int THRESHOLD = 10_000;

    public SumTask(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) sum += array[i];
            return sum;
        }
        int mid = (start + end) / 2;
        SumTask left = new SumTask(array, start, mid);
        SumTask right = new SumTask(array, mid, end);
        left.fork();           // Submit left to pool
        long rightResult = right.compute();  // Compute right in current thread
        long leftResult = left.join();       // Wait for left result
        return leftResult + rightResult;
    }
}
```

**Key concepts:**

- **`fork()`** — asynchronously submits the task to the pool.
- **`join()`** — blocks until the result is ready.
- **Work stealing** — idle threads steal tasks from busy threads' deques.
- `RecursiveTask<V>` returns a value; `RecursiveAction` does not.

---

### **Q14: Describe the thread lifecycle states.**

```
         ┌──────────────────────────────────────────────────────────────────┐
         │                  THREAD LIFECYCLE (Thread.State)                │
         └──────────────────────────────────────────────────────────────────┘

                           start()
            NEW ──────────────────────▶ RUNNABLE
                                        │    ▲
                            ┌───────────┘    │
                            │                │
                  ┌─────────▼──────┐   ┌─────┴──────────┐
                  │  synchronized  │   │  lock acquired  │
                  │  (waiting for  │   │  or notify()    │
                  │   monitor)     │   │  or interrupt() │
                  └─────────┬──────┘   └─────┬──────────┘
                            │                │
                            ▼                │
                         BLOCKED ────────────┘

                  RUNNABLE
                    │    ▲
         ┌──────────┘    │──────────────────┐
         │               │                  │
         ▼               │                  │
      WAITING      (notify/interrupt)  TIMED_WAITING
      - wait()                         - sleep(ms)
      - join()                         - wait(ms)
      - park()                         - join(ms)
                                       - parkNanos()

                  RUNNABLE ─────────▶ TERMINATED
                              (run() completes or
                               uncaught exception)
```

The six states from `Thread.State`:

| State | Entered when |
|-------|-------------|
| `NEW` | Thread created but `start()` not yet called |
| `RUNNABLE` | Running or ready to run (includes OS "running" + "ready") |
| `BLOCKED` | Waiting to acquire a monitor lock |
| `WAITING` | Waiting indefinitely (`wait()`, `join()`, `park()`) |
| `TIMED_WAITING` | Waiting with a timeout (`sleep()`, `wait(ms)`, `join(ms)`) |
| `TERMINATED` | `run()` completed or threw uncaught exception |

---

## 3. Synchronization

### **Q15: What are the two guarantees of `synchronized`?**

1. **Mutual exclusion (atomicity)** — Only one thread can execute a synchronized block guarded by the same monitor at a time. This makes compound actions atomic.

2. **Memory visibility** — When a thread exits a synchronized block, all writes made inside it are flushed to main memory. When another thread enters a synchronized block on the same monitor, it sees all those writes. This is the monitor lock happens-before rule.

Both guarantees are necessary. Mutual exclusion without visibility could leave another thread reading stale values even after acquiring the lock.

---

### **Q16: Why do reads and writes of `long` and `double` need special care?**

The JLS (§17.7) specifies that reads and writes of `long` and `double` (64-bit) values are **not guaranteed to be atomic** on 32-bit platforms. The JVM may split them into two 32-bit operations, creating a **torn read** — thread A writes the high 32 bits, thread B reads all 64 bits, producing a value that neither thread wrote.

**Fix:** Declare the field `volatile` or protect it with synchronization. In practice, most modern 64-bit JVMs perform atomic 64-bit reads/writes, but the specification does not guarantee it.

---

### **Q17: What is loop hoisting and how does it cause liveness bugs?**

Loop hoisting is a JIT optimization where a read is moved out of a loop because the compiler determines the variable is not modified within the loop (from the perspective of the current thread):

```java
// Source code
while (!done) {
    // do work
}

// JIT-compiled (hoisted)
boolean cached = done;   // One read, cached in register
while (!cached) {
    // do work — infinite loop!
}
```

This happens because without `volatile` or synchronization, the JIT is free to assume `done` doesn't change (no happens-before edge from the writer). The loop never terminates — a **liveness bug**.

---

### **Q18: What is the difference between `synchronized` methods and blocks?**

```java
// Method-level — locks on `this` (or Class object for static)
public synchronized void update() {
    // entire method body is critical section
}

// Block-level — locks on the specified object
public void update() {
    synchronized (lockObject) {
        // only this block is critical section
    }
    // Non-critical code can run here without holding the lock
}
```

| Aspect | Method | Block |
|--------|--------|-------|
| Lock object | `this` (instance) or `ClassName.class` (static) | Any specified object |
| Granularity | Entire method | Only the critical section |
| Flexibility | Cannot use different locks for different fields | Can use fine-grained locks |
| Performance | May hold lock too long | Minimizes lock scope |

**Best practice:** Prefer `synchronized` blocks with a private final lock object for fine-grained control and to avoid exposing the lock to external code.

---

### **Q19: What is a race condition? Explain the check-then-act pattern.**

A **race condition** occurs when the correctness of a computation depends on the relative timing of multiple threads. The most common form is **check-then-act**:

```java
// BROKEN — race condition!
if (balance >= amount) {        // CHECK: Thread A sees $100 >= $80
    // Thread B also checks: $100 >= $50 — both pass!
    balance = balance - amount;  // ACT: Thread A subtracts, then Thread B subtracts
}                                // Result: negative balance!
```

> **Working example:** See [Account.java](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java) — the `withdraw()` method uses `ReentrantLock` to make the check-then-act atomic.

**Other race condition patterns:**
- **Read-modify-write:** `count++` (three operations: read, increment, write)
- **Put-if-absent:** check map, then insert — solved by `ConcurrentHashMap.putIfAbsent()`

---

### **Q20: What is deadlock? How do you prevent it?**

**Deadlock** occurs when two or more threads are permanently blocked, each waiting for a lock held by the other. Four conditions must all be true:

1. **Mutual exclusion** — resources are non-sharable
2. **Hold and wait** — a thread holds one lock while requesting another
3. **No preemption** — locks cannot be forcibly taken
4. **Circular wait** — A waits for B, B waits for A

```java
// DEADLOCK EXAMPLE
Object lockA = new Object();
Object lockB = new Object();

// Thread 1
synchronized (lockA) {
    Thread.sleep(100);  // Simulate work
    synchronized (lockB) {  // Waits for Thread 2 to release lockB
        System.out.println("Thread 1");
    }
}

// Thread 2
synchronized (lockB) {
    Thread.sleep(100);
    synchronized (lockA) {  // Waits for Thread 1 to release lockA — DEADLOCK!
        System.out.println("Thread 2");
    }
}
```

**Prevention strategies:**

| Strategy | Technique |
|----------|-----------|
| **Lock ordering** | Always acquire locks in the same global order (e.g., by account ID) |
| **Timeout** | Use `tryLock(timeout, unit)` with `ReentrantLock` — give up after waiting |
| **Single lock** | Protect all shared state with one coarse lock (trades parallelism for safety) |
| **Lock-free algorithms** | Use `AtomicReference`, CAS loops — no locks, no deadlock |

**Fixed version:**

```java
// Always acquire in the same order (e.g., by hashCode or ID)
Object first = System.identityHashCode(lockA) < System.identityHashCode(lockB) ? lockA : lockB;
Object second = (first == lockA) ? lockB : lockA;

synchronized (first) {
    synchronized (second) {
        // Safe — consistent ordering
    }
}
```

---

### **Q21: What are livelock and starvation?**

**Livelock:** Threads are not blocked, but they keep responding to each other's actions without making progress. Like two people in a hallway who keep stepping aside in the same direction.

```java
// Pseudocode — two threads keep yielding to each other
while (resourceInUse) {
    Thread.yield();  // Release CPU, try again — but other thread does the same!
}
```

**Starvation:** A thread is perpetually denied access to a resource because other threads keep acquiring it first. Common with unfair locks.

**Mitigation:**
- Use `new ReentrantLock(true)` for **fair** locking (FIFO ordering).
- Add randomized back-off for livelock scenarios.

> See [Account.java](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java) — the `withdrawalLock` uses `new ReentrantLock(true)` to prevent starvation.

---

### **Q22: Explain the double-checked locking pattern and why it was broken before Java 5.**

Double-checked locking (DCL) is a singleton initialization pattern that tries to avoid synchronization after the first initialization:

```java
// BROKEN before Java 5!
public class Singleton {
    private static Singleton instance;  // NOT volatile!

    public static Singleton getInstance() {
        if (instance == null) {                // First check (no lock)
            synchronized (Singleton.class) {
                if (instance == null) {        // Second check (with lock)
                    instance = new Singleton(); // PROBLEM HERE
                }
            }
        }
        return instance;
    }
}
```

**Why it was broken:** `instance = new Singleton()` involves three steps:
1. Allocate memory
2. Initialize fields (run constructor)
3. Assign reference to `instance`

The JVM could reorder steps 2 and 3. Thread B could see a **non-null but partially constructed** `instance`, skip the `synchronized` block, and use an object whose fields are still default values.

**Java 5+ fix:** Declare `instance` as `volatile`:

```java
private static volatile Singleton instance;  // Prevents reordering
```

The volatile write of `instance` establishes a happens-before edge, ensuring all constructor writes are visible before the reference is published.

**Simpler alternative:** Use an enum singleton or the lazy holder pattern:

```java
public class Singleton {
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();  // Class loading is thread-safe
    }
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

---

## 4. Volatile Keyword

### **Q23: What does `volatile` guarantee?**

`volatile` provides two guarantees:

1. **Visibility** — A write to a volatile variable is immediately visible to all threads. No thread can cache a stale value.
2. **Ordering** — A volatile write happens-before any subsequent volatile read of the same variable. This prevents reordering of instructions around the volatile access.

Together, these ensure that when thread B reads a volatile variable and sees the value written by thread A, all writes by thread A *before* the volatile write are also visible to thread B (the piggyback effect from [Q6](#q6-explain-the-transitivity-piggyback-pattern)).

---

### **Q24: What does `volatile` NOT guarantee?**

**Atomicity of compound operations.** `volatile` makes individual reads and writes atomic, but it does NOT make read-modify-write sequences atomic:

```java
private volatile int count = 0;

// BROKEN — race condition despite volatile!
public void increment() {
    count++;  // This is: read count → add 1 → write count (three steps)
}
```

If two threads call `increment()` simultaneously:

```
Thread A: read count = 0
Thread B: read count = 0
Thread A: write count = 1
Thread B: write count = 1   // Lost update! Should be 2
```

**Fix:** Use `AtomicInteger.incrementAndGet()` or `synchronized`.

---

### **Q25: How does volatile create a happens-before edge? Show the publication flag pattern.**

A volatile write by thread A happens-before a volatile read of the same variable by thread B. Combined with transitivity, this publishes **all preceding writes**:

```java
// Thread A (producer)
data = loadFromDatabase();   // (1) Non-volatile write
config = parseConfig();       // (2) Non-volatile write
ready = true;                 // (3) VOLATILE WRITE — publishes (1) and (2)

// Thread B (consumer)
if (ready) {                  // (4) VOLATILE READ — sees (3), therefore sees (1) and (2)
    process(data);            // Guaranteed to see the loaded data
    use(config);              // Guaranteed to see the parsed config
}
```

This is called the **publication flag** pattern. The single volatile variable acts as a "barrier" that publishes all preceding non-volatile writes.

---

### **Q26: What happens at the hardware level when you use `volatile`?**

The JIT compiler emits **memory barrier** (fence) instructions:

| Barrier | Placed | Effect |
|---------|--------|--------|
| **StoreStore** | Before volatile write | Flushes all preceding stores to cache/memory |
| **StoreLoad** | After volatile write | Prevents reordering with subsequent loads |
| **LoadLoad** | After volatile read | Ensures subsequent loads see fresh values |
| **LoadStore** | After volatile read | Ensures subsequent stores cannot be reordered before the read |

On **x86** (Intel/AMD), only the `StoreLoad` barrier costs a real CPU fence instruction (`mfence` or `lock addl`). Other barriers are no-ops because x86 has a strong memory model. On **ARM** (weaker model), all barriers emit real instructions — making volatile relatively more expensive.

---

### **Q27: Compare `volatile`, `synchronized`, and `Atomic` classes.**

| Feature | `volatile` | `synchronized` | `AtomicInteger` etc. |
|---------|-----------|----------------|---------------------|
| Visibility | Yes | Yes | Yes |
| Atomicity (single r/w) | Yes | Yes | Yes |
| Atomicity (compound ops) | **No** | Yes | Yes (CAS-based) |
| Mutual exclusion | No | Yes | No |
| Blocking | No | Yes (monitor) | No (spin-retry) |
| Use case | Flags, published state | Critical sections | Counters, accumulators |
| Performance | Low overhead | Higher overhead (context switches) | Medium (CAS retries under contention) |

**Rule of thumb:**
- **Volatile** for simple flags and published references (no compound operations).
- **Atomic** classes for counters and single-variable CAS operations.
- **Synchronized/Lock** for multi-variable invariants and check-then-act patterns.

---

### **Q28: Does `volatile` fix torn reads for `long` and `double`?**

Yes. Declaring a `long` or `double` field as `volatile` guarantees that reads and writes are **atomic** (no torn reads/writes), even on 32-bit platforms. This is explicitly stated in JLS §17.7:

> *"Writes and reads of volatile long and double values are always atomic."*

---

## 5. Stopping Threads Safely

### **Q29: Why is `Thread.stop()` deprecated?**

`Thread.stop()` is **inherently unsafe** because it throws a `ThreadDeath` error at an unpredictable point, which:

1. **Releases all monitors** the thread holds, leaving objects in an inconsistent state.
2. **Cannot be caught reliably** — even if caught, the damage may already be done.
3. **Violates invariants** — a thread might be in the middle of updating two related fields.

Since Java 1.2, `Thread.stop()` has been deprecated. Since Java 20, it throws `UnsupportedOperationException`.

---

### **Q30: How do you stop a thread using a cooperative volatile flag?**

```java
public class Worker implements Runnable {
    private volatile boolean cancelled = false;  // Volatile for visibility

    @Override
    public void run() {
        while (!cancelled) {
            doUnitOfWork();
        }
        cleanup();  // Orderly shutdown
    }

    public void cancel() {
        cancelled = true;  // Visible to worker thread immediately
    }
}
```

**Limitation:** This only works if the thread regularly checks the flag. If the thread is blocked on `sleep()`, `wait()`, or I/O, it won't check the flag until the blocking call returns.

---

### **Q31: How does `Thread.interrupt()` work?**

`Thread.interrupt()` is the standard cooperative cancellation mechanism:

1. If the thread is **blocked** on `sleep()`, `wait()`, `join()`, or an interruptible I/O call, the blocking method throws `InterruptedException` and clears the interrupt status.
2. If the thread is **running**, the interrupt flag is set to `true`. The thread must check `Thread.interrupted()` or `isInterrupted()` periodically.

```java
public void run() {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            doWork();
            Thread.sleep(100);  // Throws InterruptedException if interrupted
        }
    } catch (InterruptedException e) {
        // Thread was interrupted during sleep — clean up and exit
        Thread.currentThread().interrupt();  // Restore flag for callers
    }
    cleanup();
}
```

---

### **Q32: What is the difference between `interrupted()` and `isInterrupted()`?**

| Method | Static? | Clears flag? | Use case |
|--------|---------|-------------|----------|
| `Thread.interrupted()` | Yes (checks **current** thread) | **Yes** — clears the flag | Use when you plan to handle the interrupt and take action |
| `thread.isInterrupted()` | No (checks **target** thread) | **No** — preserves the flag | Use when checking status without consuming the signal |

**Common mistake:** Calling `Thread.interrupted()` in a loop condition without realizing it clears the flag on the first `true` return, making subsequent checks return `false`.

---

### **Q33: How should you handle `InterruptedException`?**

Two correct approaches:

**1. Propagate it** (preferred when possible):

```java
public void doWork() throws InterruptedException {
    Thread.sleep(1000);  // Let the exception propagate up
}
```

**2. Restore the interrupt flag** (when you can't propagate):

```java
public void run() {  // Runnable.run() can't declare throws
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // Restore the flag!
        return;  // Exit gracefully
    }
}
```

**Never do this:**

```java
catch (InterruptedException e) {
    // Swallowing the interrupt — WRONG!
    // Callers will never know the thread was interrupted
}
```

---

## 6. Joining Threads

### **Q34: What does `Thread.join()` do and what is its happens-before guarantee?**

`Thread.join()` blocks the calling thread until the target thread terminates. It provides a **happens-before** guarantee: all actions in the joined thread are visible to the thread that called `join()` after it returns.

```java
int[] result = new int[1];
Thread worker = new Thread(() -> result[0] = computeExpensiveValue());
worker.start();
worker.join();                    // Block until worker finishes
System.out.println(result[0]);   // Guaranteed to see the computed value
```

Without `join()` (or another synchronization mechanism), `result[0]` might still be `0`.

---

### **Q35: What overloads does `join()` have?**

| Method | Behavior |
|--------|----------|
| `join()` | Wait indefinitely until the thread dies |
| `join(long millis)` | Wait at most `millis` milliseconds. Returns even if thread is still alive. |
| `join(long millis, int nanos)` | Higher precision timeout (rarely used) |

**Important:** After a timed `join()`, you must check `thread.isAlive()` to determine if the thread actually finished:

```java
worker.join(5000);  // Wait up to 5 seconds
if (worker.isAlive()) {
    System.out.println("Worker is still running — timed out");
    worker.interrupt();  // Request cancellation
}
```

---

### **Q36: Compare `CompletableFuture.join()` vs `Future.get()`.**

| Feature | `Future.get()` | `CompletableFuture.join()` |
|---------|----------------|---------------------------|
| Checked exception | Throws `InterruptedException`, `ExecutionException` | None (wraps in `CompletionException`) |
| Timeout overload | `get(long, TimeUnit)` | No built-in timeout (use `orTimeout()` since Java 9) |
| Unchecked exception | No | Yes — `CompletionException` |
| Use in streams | Awkward (must catch checked exceptions) | Clean — no try/catch needed |

```java
// Future.get() — verbose
try {
    String result = future.get(5, TimeUnit.SECONDS);
} catch (InterruptedException | ExecutionException | TimeoutException e) {
    handleError(e);
}

// CompletableFuture.join() — clean
String result = completableFuture.join();  // Throws unchecked CompletionException
```

---

### **Q37: How does `CountDownLatch` work as a flexible coordination mechanism?**

A `CountDownLatch` is initialized with a count. Threads call `await()` to block until the count reaches zero. Other threads call `countDown()` to decrement the count.

**Two common patterns:**

**1. Starting gun** (1 → many) — one latch coordinates many threads starting simultaneously:

```java
CountDownLatch startLatch = new CountDownLatch(1);
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        startLatch.await();  // All threads wait here
        doWork();
    });
}
startLatch.countDown();  // Fire! All threads released at once
```

**2. Finish line** (many → 1) — wait for all threads to complete:

```java
CountDownLatch doneLatch = new CountDownLatch(numThreads);
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        try { doWork(); }
        finally { doneLatch.countDown(); }
    });
}
doneLatch.await();  // Block until all threads finish
```

> **Working example:** See [TransactionValidatorTest.java](src/test/java/howToSolveCodingProblems/transactionValidator/TransactionValidatorTest.java) — the concurrency tests use both patterns together (starting gun + finish line) to test 100 concurrent withdrawals.

---

## 7. Executor Framework & Queues

### **Q38: What are the standard `ExecutorService` implementations?**

| Factory Method | Pool Type | Queue | Use Case |
|---------------|-----------|-------|----------|
| `newFixedThreadPool(n)` | Fixed `n` threads | Unbounded `LinkedBlockingQueue` | General-purpose server workloads |
| `newCachedThreadPool()` | 0 → `Integer.MAX_VALUE` threads | `SynchronousQueue` (no capacity) | Bursty short-lived tasks |
| `newSingleThreadExecutor()` | 1 thread | Unbounded `LinkedBlockingQueue` | Sequential task processing |
| `newScheduledThreadPool(n)` | Fixed `n` core threads | `DelayedWorkQueue` | Periodic / delayed tasks |
| `newWorkStealingPool()` | CPUs × 1 (ForkJoinPool) | Work-stealing deques | CPU-bound parallel computation |

---

### **Q39: How do task queues work in `ThreadPoolExecutor`?**

When you submit a task to a `ThreadPoolExecutor`:

1. If **fewer than `corePoolSize`** threads exist → create a new thread to run the task.
2. If core threads are full → **enqueue** the task in the work queue.
3. If the queue is full → create a new thread up to `maximumPoolSize`.
4. If `maximumPoolSize` is reached and queue is full → invoke the `RejectedExecutionHandler`.

```
submit(task)
    │
    ▼
corePoolSize reached?
    │ No → Create new thread
    │ Yes ▼
    Queue full?
    │ No → Enqueue task
    │ Yes ▼
    maxPoolSize reached?
    │ No → Create new thread
    │ Yes ▼
    RejectedExecutionHandler
```

---

### **Q40: What is the difference between bounded and unbounded queues?**

| Queue Type | Behavior | Risk |
|-----------|----------|------|
| **Unbounded** (`LinkedBlockingQueue()`) | Always accepts new tasks | `OutOfMemoryError` if tasks pile up faster than they complete |
| **Bounded** (`ArrayBlockingQueue(capacity)`) | Blocks or rejects when full | Provides **back-pressure** — forces producers to slow down |
| **Zero-capacity** (`SynchronousQueue`) | No buffering; handoff only | Every submit must find a waiting thread immediately |

**Best practice for production:** Always use bounded queues with an explicit rejection policy.

---

### **Q41: What are the four `RejectedExecutionHandler` policies?**

| Policy | Behavior |
|--------|----------|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` |
| `CallerRunsPolicy` | Runs the task in the calling thread — natural back-pressure! |
| `DiscardPolicy` | Silently discards the task |
| `DiscardOldestPolicy` | Discards the oldest queued task, then retries submit |

```java
var executor = new ThreadPoolExecutor(
    4, 8, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy()  // Back-pressure: caller slows down
);
```

---

### **Q42: Why are `Executors` factory methods considered dangerous for production?**

| Factory Method | Problem |
|---------------|---------|
| `newFixedThreadPool(n)` | Uses **unbounded** `LinkedBlockingQueue` → OOM risk |
| `newSingleThreadExecutor()` | Uses **unbounded** `LinkedBlockingQueue` → OOM risk |
| `newCachedThreadPool()` | Creates up to **`Integer.MAX_VALUE`** threads → thread exhaustion |
| `newScheduledThreadPool(n)` | Uses **unbounded** `DelayedWorkQueue` → OOM risk |

**Best practice:** Create `ThreadPoolExecutor` directly with explicit parameters:

```java
var executor = new ThreadPoolExecutor(
    4,                                   // corePoolSize
    8,                                   // maximumPoolSize
    60, TimeUnit.SECONDS,                // keepAliveTime for idle threads
    new ArrayBlockingQueue<>(1000),      // BOUNDED queue
    new CustomThreadFactory("worker"),   // Named threads for debugging
    new ThreadPoolExecutor.CallerRunsPolicy()  // Rejection policy
);
```

---

### **Q43: How do you implement back-pressure in a producer-consumer system?**

Back-pressure means the consumer signals the producer to slow down when overwhelmed. Java provides several mechanisms:

1. **Bounded `BlockingQueue`** — `producer.put(item)` blocks when the queue is full.
2. **`CallerRunsPolicy`** — the submitting thread runs the task itself, naturally slowing down submission.
3. **Semaphore** — limit the number of in-flight tasks.

```java
// Semaphore-based back-pressure
Semaphore permits = new Semaphore(100);  // Max 100 in-flight tasks

void submitTask(Runnable task) throws InterruptedException {
    permits.acquire();  // Block if 100 tasks already in flight
    try {
        executor.submit(() -> {
            try { task.run(); }
            finally { permits.release(); }  // Return permit when done
        });
    } catch (RejectedExecutionException e) {
        permits.release();  // Don't leak permits
        throw e;
    }
}
```

---

### **Q44: Why are task queues poor for ultra-low-latency systems?**

Standard `BlockingQueue` implementations use locks (`ReentrantLock`) internally, which cause:

1. **Context switches** — blocked threads yield CPU time.
2. **Cache invalidation** — lock state bounces between CPU caches.
3. **Unpredictable latency** — GC pauses + lock contention = tail latency spikes.

**Alternative:** The **LMAX Disruptor** uses a lock-free ring buffer with pre-allocated entries, achieving < 1 microsecond latency vs. ~5-10 microseconds for `ArrayBlockingQueue`.

---

### **Q45: What is the correct way to shut down an `ExecutorService`?**

Use the **two-phase shutdown** pattern:

```java
void shutdownExecutor(ExecutorService executor) {
    executor.shutdown();  // Phase 1: Stop accepting new tasks
    try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();  // Phase 2: Interrupt running tasks
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate");
            }
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();  // Restore interrupt flag
    }
}
```

| Method | Effect |
|--------|--------|
| `shutdown()` | Stops accepting new tasks; existing tasks run to completion |
| `shutdownNow()` | Attempts to interrupt running tasks; returns list of unexecuted tasks |
| `awaitTermination()` | Blocks until all tasks complete or timeout expires |

---

## 8. CompletableFuture & Advanced Async

### **Q46: How does `CompletableFuture` improve over `Future`?**

| Feature | `Future` | `CompletableFuture` |
|---------|----------|---------------------|
| Completion check | `isDone()` (polling) | Callbacks (`thenApply`, `thenAccept`, etc.) |
| Chaining | Not possible | `thenApply().thenCompose().thenAccept()` |
| Combining | Manual | `allOf()`, `anyOf()`, `thenCombine()` |
| Exception handling | `get()` wraps in `ExecutionException` | `exceptionally()`, `handle()`, `whenComplete()` |
| Manual completion | Not possible | `complete(value)`, `completeExceptionally(ex)` |
| Non-blocking | `get()` always blocks | Callbacks are non-blocking |

---

### **Q47: What is the difference between `thenApply()` and `thenCompose()`?**

This is the **map vs. flatMap** distinction:

```java
// thenApply = map: transforms the value, returns a plain value
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 42)
    .thenApply(i -> "Result: " + i);  // int → String

// thenCompose = flatMap: transforms the value, returns another CompletableFuture
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 42)
    .thenCompose(i -> fetchFromDb(i));  // int → CompletableFuture<String>
```

**Key difference:**
- `thenApply(Function<T, U>)` → `CompletableFuture<U>`
- `thenCompose(Function<T, CompletableFuture<U>>)` → `CompletableFuture<U>` (flattened)

If you use `thenApply` with a function that returns a `CompletableFuture`, you get `CompletableFuture<CompletableFuture<U>>` — nested and unwieldy.

---

### **Q48: What is the difference between `thenApply()` and `thenApplyAsync()`?**

| Method | Execution thread |
|--------|-----------------|
| `thenApply(fn)` | Runs `fn` on the **thread that completed the previous stage** (or the calling thread if already complete) |
| `thenApplyAsync(fn)` | Runs `fn` on the **common ForkJoinPool** (or specified executor) |
| `thenApplyAsync(fn, executor)` | Runs `fn` on the **given executor** |

**Guideline:**
- Use non-async variants for fast, CPU-light transformations.
- Use async variants for blocking or heavy operations to avoid blocking the completing thread.

---

### **Q49: How do you handle exceptions in a `CompletableFuture` chain?**

Three mechanisms:

```java
CompletableFuture.supplyAsync(() -> riskyOperation())

    // exceptionally: recover from failure, return default
    .exceptionally(ex -> {
        log.error("Failed", ex);
        return defaultValue;
    })

    // handle: access both result and exception
    .handle((result, ex) -> {
        if (ex != null) return fallback;
        return transform(result);
    })

    // whenComplete: side effects only, does not transform result
    .whenComplete((result, ex) -> {
        if (ex != null) alertOps(ex);
        else updateMetrics(result);
    });
```

| Method | Can recover? | Transforms result? | Receives both result and exception? |
|--------|-------------|-------------------|--------------------------------------|
| `exceptionally(fn)` | Yes | Only on failure | No — only exception |
| `handle(fn)` | Yes | Yes | Yes — both (one is null) |
| `whenComplete(fn)` | No | No | Yes — both (one is null) |

---

### **Q50: How do you combine multiple `CompletableFuture` instances?**

**Fan-out / fan-in pattern:**

```java
CompletableFuture<String> userFuture   = fetchUser(userId);
CompletableFuture<List<Order>> orders  = fetchOrders(userId);
CompletableFuture<Double> creditScore  = fetchCreditScore(userId);

// allOf — wait for all, then combine results
CompletableFuture<Void> all = CompletableFuture.allOf(userFuture, orders, creditScore);

CompletableFuture<UserProfile> profile = all.thenApply(v ->
    new UserProfile(
        userFuture.join(),
        orders.join(),
        creditScore.join()
    )
);
```

**Other combining methods:**

| Method | Description |
|--------|-------------|
| `thenCombine(other, fn)` | Combine two futures when both complete |
| `allOf(cf1, cf2, ...)` | Wait for all to complete (returns `Void`) |
| `anyOf(cf1, cf2, ...)` | Complete when any one completes (returns `Object`) |
| `thenAcceptBoth(other, fn)` | Like `thenCombine` but no return value |

---

### **Q51: Why is using `ForkJoinPool.commonPool()` as the default executor a concern?**

`CompletableFuture.supplyAsync()` (without an explicit executor) runs on `ForkJoinPool.commonPool()`, which is:

1. **Shared across the entire JVM** — parallel streams, other `CompletableFuture` calls, and your code all compete for the same threads.
2. **Sized to `Runtime.getRuntime().availableProcessors() - 1`** — typically small (e.g., 7 threads on an 8-core machine).
3. **Not designed for blocking I/O** — a few blocking calls can saturate the pool.

**Best practice:** Always provide an explicit executor for I/O-bound work:

```java
ExecutorService ioPool = Executors.newFixedThreadPool(20);
CompletableFuture.supplyAsync(() -> callExternalApi(), ioPool);
```

---

### **Q52: When should you use the `Async` suffix variants?**

Use `thenApplyAsync()`, `thenComposeAsync()`, etc. when:

1. The transformation is **CPU-intensive** and you don't want to block the thread that completed the previous stage.
2. The transformation involves **blocking I/O** (database call, HTTP request).
3. You need to run on a **specific executor** (e.g., a dedicated I/O pool).

Skip the `Async` suffix when the transformation is a lightweight, non-blocking operation (e.g., mapping a field, formatting a string).

---

## 9. Lock API & Synchronization Utilities

### **Q53: Compare `ReentrantLock` vs `synchronized`.**

| Feature | `synchronized` | `ReentrantLock` |
|---------|---------------|-----------------|
| Syntax | Built-in keyword | API (`lock()` / `unlock()`) |
| Unlock | Automatic (end of block) | Manual — **must** use `finally` |
| Timeout | No | `tryLock(time, unit)` |
| Interruptible | No | `lockInterruptibly()` |
| Fairness | No (always unfair) | Configurable: `new ReentrantLock(true)` |
| Multiple conditions | One implicit (`wait/notify`) | Multiple via `newCondition()` |
| Lock status query | No | `isLocked()`, `isHeldByCurrentThread()`, `getQueueLength()` |
| Performance (uncontended) | Similar (biased locking removed in Java 15+) | Similar |

> **Working example:** See [Account.java](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java) — `withdrawalLock` is a `ReentrantLock(true)` that protects the check-then-act withdrawal.

**When to prefer `ReentrantLock`:** timeouts, interruptible locks, fairness, multiple conditions, or diagnostic queries.

**When to prefer `synchronized`:** simple critical sections where automatic unlock is valued and no advanced features are needed.

---

### **Q54: How does `ReadWriteLock` improve concurrency for read-heavy workloads?**

`ReadWriteLock` allows **multiple concurrent readers** but only **one writer** (and no readers while writing):

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Map<String, String> cache = new HashMap<>();

// Multiple threads can read concurrently
public String get(String key) {
    rwLock.readLock().lock();
    try {
        return cache.get(key);
    } finally {
        rwLock.readLock().unlock();
    }
}

// Only one thread can write; all readers are blocked during write
public void put(String key, String value) {
    rwLock.writeLock().lock();
    try {
        cache.put(key, value);
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

**Benefit:** In a 90% read / 10% write workload, readers proceed in parallel — dramatically better throughput than a single exclusive lock.

**Caution:** Writers can be starved if readers keep acquiring the read lock. Use a **fair** `ReentrantReadWriteLock(true)` if writer starvation is a concern.

---

### **Q55: What is `StampedLock` and what are optimistic reads?**

`StampedLock` (Java 8+) adds an **optimistic read** mode that doesn't acquire any lock:

```java
StampedLock sl = new StampedLock();
double x, y;

// OPTIMISTIC READ — no locking overhead!
public double distanceFromOrigin() {
    long stamp = sl.tryOptimisticRead();  // Non-blocking "read attempt"
    double currentX = x, currentY = y;    // Read shared state

    if (!sl.validate(stamp)) {            // Check if a write occurred
        // Fallback to pessimistic read lock
        stamp = sl.readLock();
        try {
            currentX = x;
            currentY = y;
        } finally {
            sl.unlockRead(stamp);
        }
    }
    return Math.sqrt(currentX * currentX + currentY * currentY);
}

// Write lock (exclusive)
public void move(double deltaX, double deltaY) {
    long stamp = sl.writeLock();
    try {
        x += deltaX;
        y += deltaY;
    } finally {
        sl.unlockWrite(stamp);
    }
}
```

**Optimistic reads are ideal** when writes are rare and read operations are short. If a write occurs during the optimistic read, `validate()` returns `false` and you fall back to a real lock.

**Important:** `StampedLock` is **not reentrant** and does **not** support `Condition` objects.

---

### **Q56: How do `Condition` objects replace `wait()`/`notify()`?**

`Condition` (from `ReentrantLock.newCondition()`) provides a more flexible version of `wait()`/`notify()`:

```java
ReentrantLock lock = new ReentrantLock();
Condition notEmpty = lock.newCondition();
Condition notFull  = lock.newCondition();
Queue<Item> queue = new LinkedList<>();
int capacity = 10;

public void put(Item item) throws InterruptedException {
    lock.lock();
    try {
        while (queue.size() == capacity)
            notFull.await();           // Wait on "not full" condition
        queue.add(item);
        notEmpty.signal();             // Signal "not empty" condition
    } finally {
        lock.unlock();
    }
}

public Item take() throws InterruptedException {
    lock.lock();
    try {
        while (queue.isEmpty())
            notEmpty.await();          // Wait on "not empty" condition
        Item item = queue.remove();
        notFull.signal();              // Signal "not full" condition
        return item;
    } finally {
        lock.unlock();
    }
}
```

**Advantages over `wait()`/`notify()`:**
- **Multiple conditions per lock** — producers wait on `notFull`, consumers wait on `notEmpty`.
- **Avoids wasted wakeups** — `signal()` wakes the right thread, not a random waiter.

---

### **Q57: How does `Semaphore` work for resource limiting?**

A `Semaphore` maintains a set of **permits**. `acquire()` takes a permit (blocking if none available), `release()` returns one.

```java
// Limit to 5 concurrent database connections
Semaphore connectionPool = new Semaphore(5, true);  // fair=true

public Connection getConnection() throws InterruptedException {
    connectionPool.acquire();  // Block if all 5 permits are taken
    return pool.borrowConnection();
}

public void returnConnection(Connection conn) {
    pool.returnConnection(conn);
    connectionPool.release();  // Return permit
}
```

**Use cases:** connection pools, rate limiters, bounded resource access.

---

### **Q58: Compare `CountDownLatch`, `CyclicBarrier`, and `Phaser`.**

| Feature | `CountDownLatch` | `CyclicBarrier` | `Phaser` |
|---------|-----------------|-----------------|----------|
| Reusable | **No** — single use | **Yes** — resets after each barrier | **Yes** — advances to next phase |
| Direction | N → 1 (wait for N events) | N ↔ N (all parties meet) | N ↔ N (dynamic registration) |
| Dynamic parties | No | No (fixed at creation) | **Yes** — `register()` / `arriveAndDeregister()` |
| Barrier action | None | Optional `Runnable` when all arrive | Override `onAdvance()` |
| Typical use | Wait for N tasks to complete | Iterative parallel algorithms | Multi-phase algorithms with dynamic participants |
| Example | Starting gun pattern | Matrix row computation | MapReduce-style processing |

> **Working example:** See [TransactionValidatorTest.java](src/test/java/howToSolveCodingProblems/transactionValidator/TransactionValidatorTest.java) — `CountDownLatch` is used as both a starting gun and a finish line.

---

## 10. Atomic Classes & Lock-Free Programming

### **Q59: What atomic classes does `java.util.concurrent.atomic` provide?**

| Class | Description |
|-------|-------------|
| `AtomicInteger`, `AtomicLong` | Atomic int/long with CAS operations |
| `AtomicBoolean` | Atomic boolean flag |
| `AtomicReference<V>` | Atomic reference to an object |
| `AtomicIntegerArray`, `AtomicLongArray`, `AtomicReferenceArray` | Atomic operations on array elements |
| `AtomicStampedReference<V>` | Reference + int stamp (solves ABA problem) |
| `AtomicMarkableReference<V>` | Reference + boolean mark |
| `LongAdder`, `DoubleAdder` | High-contention accumulators (Java 8+) |
| `LongAccumulator`, `DoubleAccumulator` | Generalized accumulators with custom functions |

> **Working example:** See [Account.java](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java) — uses `AtomicReference<BigDecimal>` for the account balance.

---

### **Q60: How does Compare-And-Swap (CAS) work internally?**

CAS is a CPU-level atomic instruction (`CMPXCHG` on x86) that atomically:

1. **Reads** the current value at a memory location.
2. **Compares** it to an expected value.
3. **Sets** it to a new value *only if* the current value equals the expected value.
4. **Returns** whether the swap succeeded.

```java
// Manual CAS loop — this is what AtomicReference.updateAndGet() does internally
public void deposit(BigDecimal amount) {
    BigDecimal current;
    BigDecimal updated;
    do {
        current = balance.get();                          // 1. Read
        updated = current.add(amount);                    // 2. Compute
    } while (!balance.compareAndSet(current, updated));   // 3. CAS — retry on failure
}
```

> **Working example:** See [Account.deposit()](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java) — uses `balance.updateAndGet(current -> current.add(amount))` which is equivalent to the manual loop above.

---

### **Q61: What is the ABA problem and how does `AtomicStampedReference` solve it?**

**ABA problem:** Thread A reads value `A`, gets preempted. Thread B changes `A → B → A`. Thread A wakes up, CAS sees `A` (matches expected) and succeeds — but the value has been logically modified.

In most Java use cases (counters, accumulators) ABA is harmless. But in **lock-free data structures** (e.g., lock-free stacks), ABA can cause corruption.

**Solution:** `AtomicStampedReference` pairs the reference with an integer **stamp** (version counter):

```java
AtomicStampedReference<Node> head = new AtomicStampedReference<>(null, 0);

// CAS now checks both reference AND stamp
int[] stampHolder = new int[1];
Node current = head.get(stampHolder);
int currentStamp = stampHolder[0];

// Even if reference is the same, stamp must also match
head.compareAndSet(current, newNode, currentStamp, currentStamp + 1);
```

---

### **Q62: When should you use `LongAdder` instead of `AtomicLong`?**

| Scenario | `AtomicLong` | `LongAdder` |
|----------|-------------|-------------|
| Low contention | Excellent | Slight overhead (more memory) |
| **High contention** | CAS retries degrade throughput | **Much better** — cells reduce contention |
| Read frequency | `get()` is O(1) | `sum()` is O(cells) — slightly slower |
| Use case | Single counter, few writers | Metrics, statistics, many writers |

**How `LongAdder` works:** Instead of one shared variable, it maintains an array of **cells** (like a striped lock). Each thread typically writes to its own cell, avoiding CAS contention. `sum()` adds all cells together.

```java
// High-contention counter — use LongAdder
LongAdder requestCount = new LongAdder();

// From many threads:
requestCount.increment();  // Minimal contention

// Periodic read:
long total = requestCount.sum();  // Sums all cells
```

**Benchmark:** Under 16+ threads, `LongAdder` can be **10x faster** than `AtomicLong`.

---

### **Q63: What is the difference between lock-free and wait-free?**

| Property | Definition | Guarantee |
|----------|-----------|-----------|
| **Lock-free** | At least one thread makes progress in a finite number of steps | System-wide progress; individual threads may starve |
| **Wait-free** | Every thread completes in a bounded number of steps | No starvation — strongest guarantee |
| **Obstruction-free** | A thread makes progress if no other threads are active | Weakest; requires contention management |

Most `java.util.concurrent.atomic` classes are **lock-free** (CAS retry loops). `LongAdder` is effectively **wait-free** for increments because each thread writes to its own cell.

True wait-free algorithms are rare and complex. In practice, lock-free algorithms with CAS are sufficient for most applications.

---

## 11. Concurrent Collections & ThreadLocal

### **Q64: Compare `ConcurrentHashMap` vs `Collections.synchronizedMap()`.**

| Feature | `synchronizedMap` | `ConcurrentHashMap` |
|---------|-------------------|---------------------|
| Lock granularity | **Single lock** on entire map | **Segment/node-level** locking |
| Read concurrency | Readers block each other | **Multiple concurrent readers** |
| Write concurrency | Serialized (one at a time) | **Concurrent writes** to different segments |
| Atomic compound ops | No — must externally synchronize | `putIfAbsent`, `computeIfAbsent`, `merge` |
| Null keys/values | Allowed | **Not allowed** |
| Iterator | Fail-fast (`ConcurrentModificationException`) | **Weakly consistent** (no exception) |
| Scalability | Poor under contention | Excellent under contention |

> **Working example:** See [TransactionValidator.java](src/main/java/howToSolveCodingProblems/transactionValidator/TransactionValidator.java) — uses `ConcurrentHashMap<String, Account>` for the account registry. The `putIfAbsent()` call in `createAccount()` is atomic.

---

### **Q65: How do you perform atomic compound operations on `ConcurrentHashMap`?**

`ConcurrentHashMap` provides several atomic compound operations:

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// putIfAbsent — atomic check-and-put
map.putIfAbsent("key", 1);  // Only puts if key absent

// computeIfAbsent — atomic check-and-compute (lazy init)
map.computeIfAbsent("key", k -> expensiveComputation(k));

// merge — atomic read-modify-write
map.merge("key", 1, Integer::sum);  // Increment or insert 1

// compute — atomic update or remove
map.compute("key", (k, v) -> v == null ? 1 : v + 1);
```

> **Working example:** See [Account.markProcessed()](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java) — `processedTransactions.putIfAbsent(transactionId, Instant.now())` atomically checks for duplicates and records the timestamp.

**Critical:** Do NOT use `if (!map.containsKey(k)) map.put(k, v)` — this is a **check-then-act race condition**. Always use the atomic compound methods.

---

### **Q66: When should you use `CopyOnWriteArrayList`?**

`CopyOnWriteArrayList` creates a **new copy of the underlying array** on every write (add, set, remove). Reads are never blocked.

**Ideal for:**
- **Listeners/observers** — registered once, iterated many times.
- **Configuration lists** — read frequently, updated rarely.
- Event handler registries.

**Not suitable for:**
- Large lists with frequent writes (each write copies the entire array — O(n)).
- High write-to-read ratio.

```java
CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

// Thread-safe iteration — no ConcurrentModificationException
for (EventListener listener : listeners) {
    listener.onEvent(event);  // Safe even if another thread adds/removes a listener
}
```

---

### **Q67: Compare `BlockingQueue` implementations.**

| Implementation | Bounded? | Ordering | Lock Structure | Best For |
|---------------|----------|----------|---------------|----------|
| `ArrayBlockingQueue` | Yes (fixed) | FIFO | Single lock | General bounded queue |
| `LinkedBlockingQueue` | Optional (default unbounded) | FIFO | Two locks (put + take) | Higher throughput than ABQ |
| `PriorityBlockingQueue` | No (unbounded) | Priority (natural or Comparator) | Single lock | Task prioritization |
| `SynchronousQueue` | Zero capacity | Direct handoff | Lock-free (transfer) | Thread-to-thread handoff |
| `LinkedTransferQueue` | No (unbounded) | FIFO | Lock-free | High-perf producer-consumer |
| `DelayQueue` | No (unbounded) | Delay-based | Single lock | Scheduled/delayed tasks |

**`LinkedBlockingQueue`** has two separate locks for `put` and `take`, allowing concurrent producers and consumers. `ArrayBlockingQueue` uses a single lock, serializing all access.

---

### **Q68: What are `ThreadLocal` pitfalls, especially in thread pools?**

`ThreadLocal` gives each thread its own copy of a variable:

```java
ThreadLocal<SimpleDateFormat> dateFormat =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
```

**Pitfalls:**

1. **Memory leak in thread pools:** Pool threads are long-lived. `ThreadLocal` values are held by the thread's `ThreadLocalMap`. If you don't call `threadLocal.remove()`, values accumulate.

```java
// WRONG — leaks in thread pools
executor.submit(() -> {
    dateFormat.set(new SimpleDateFormat("yyyy-MM-dd"));
    process();
    // Forgot to call dateFormat.remove()!
    // The SimpleDateFormat stays attached to the pool thread forever
});

// CORRECT — always clean up
executor.submit(() -> {
    try {
        dateFormat.set(new SimpleDateFormat("yyyy-MM-dd"));
        process();
    } finally {
        dateFormat.remove();  // Clean up!
    }
});
```

2. **Surprising behavior with virtual threads (Java 21+):** Virtual threads are cheap and numerous — using `ThreadLocal` with them can cause excessive memory usage. Consider `ScopedValue` (preview in Java 21+) instead.

3. **Hidden coupling:** `ThreadLocal` is essentially a global mutable variable scoped to a thread. It makes code harder to test and reason about.

---

## 12. Virtual Threads & Modern Concurrency (Java 21+)

> **Note:** This section covers features from **Java 21+**. Virtual threads are GA in Java 21. Structured concurrency (`StructuredTaskScope`) is a preview feature.

### **Q69: What are virtual threads and how do they differ from platform threads?**

| Aspect | Platform Thread | Virtual Thread |
|--------|----------------|----------------|
| Backed by | OS kernel thread (1:1) | JVM-managed (M:N scheduling) |
| Memory cost | ~1 MB stack per thread | ~few KB (grows on demand) |
| Creation cost | Expensive (OS kernel call) | Cheap (~1 μs) |
| Max practical count | ~thousands | **Millions** |
| Scheduling | OS scheduler | JVM scheduler (ForkJoinPool carrier threads) |
| Blocking cost | Expensive (blocks OS thread) | Cheap (unmounts from carrier) |
| Use case | CPU-bound, long-running | **I/O-bound**, short-lived tasks |

**Key insight:** When a virtual thread blocks on I/O (`socket.read()`, `Thread.sleep()`, etc.), it is **unmounted** from its carrier (platform) thread, freeing it for other virtual threads. This is why millions of virtual threads can coexist without millions of OS threads.

---

### **Q70: How do you create virtual threads?**

Three ways:

```java
// 1. Thread.ofVirtual()
Thread vt = Thread.ofVirtual()
    .name("worker-", 0)
    .start(() -> doWork());

// 2. Virtual thread per-task executor (preferred for structured use)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> fetchUrl("https://example.com"));
    executor.submit(() -> queryDatabase());
    // Each task gets its own virtual thread
}  // AutoCloseable — waits for tasks to complete

// 3. Thread.startVirtualThread (convenience)
Thread.startVirtualThread(() -> doWork());
```

**Best practice:** Use `Executors.newVirtualThreadPerTaskExecutor()` — it integrates with the existing `ExecutorService` API and provides structured lifecycle management.

---

### **Q71: What is structured concurrency?**

**Structured concurrency** (preview in Java 21+) ensures that the lifetime of concurrent tasks is bounded by the scope of the code that created them — similar to how structured programming bounded control flow.

```java
// Java 21+ (preview)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<String> user   = scope.fork(() -> fetchUser(userId));
    Subtask<List<Order>> orders = scope.fork(() -> fetchOrders(userId));

    scope.join();            // Wait for all subtasks
    scope.throwIfFailed();   // Propagate first failure

    // Both completed successfully
    return new UserProfile(user.get(), orders.get());

}  // If any subtask fails, others are cancelled automatically
```

**Benefits:**
1. **No leaked threads** — all forked tasks complete before the scope exits.
2. **Automatic cancellation** — if one task fails, siblings are cancelled.
3. **Clear parent-child relationship** — visible in thread dumps and debuggers.
4. **Error propagation** — exceptions flow naturally to the parent scope.

---

### **Q72: What are virtual thread gotchas?**

1. **Pinning:** A virtual thread is "pinned" to its carrier thread when inside a `synchronized` block or calling native code via JNI. While pinned, no other virtual thread can use that carrier. **Fix:** Replace `synchronized` with `ReentrantLock` for I/O-heavy code.

```java
// BAD with virtual threads — pins the carrier
synchronized (lock) {
    socket.read();  // Blocks while pinned — wastes a carrier thread
}

// GOOD with virtual threads — does not pin
reentrantLock.lock();
try {
    socket.read();  // Virtual thread unmounts; carrier is free
} finally {
    reentrantLock.unlock();
}
```

2. **ThreadLocal memory:** Each virtual thread has its own `ThreadLocal` map. With millions of virtual threads, this can exhaust memory. **Fix:** Use `ScopedValue` (preview) instead.

3. **CPU-bound work:** Virtual threads don't help with CPU-bound tasks — they still need carrier threads. Use platform thread pools for compute-intensive work.

4. **Pool sizing becomes irrelevant:** With virtual threads, you don't tune pool sizes. Create one virtual thread per task — the JVM handles scheduling.

---

### **Q73: How do virtual threads change traditional thread pool advice?**

| Old Advice (Platform Threads) | New Reality (Virtual Threads) |
|------------------------------|-------------------------------|
| "Size your pool to N × CPU cores" | Create one virtual thread per task |
| "Use thread pools to limit concurrency" | Use `Semaphore` to limit concurrency |
| "Avoid creating too many threads" | Create millions of virtual threads freely |
| "Use async/reactive for I/O-bound work" | Use simple blocking code with virtual threads |
| "Choose between reactive and thread-per-request" | Thread-per-request is efficient again |

**Important:** Virtual threads do NOT replace the need for concurrency control (locks, atomics, etc.). They change *resource management*, not *correctness concerns*.

```java
// Traditional: Limited thread pool + async I/O
ExecutorService pool = Executors.newFixedThreadPool(200);

// Modern (Java 21+): Virtual threads + simple blocking code
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Semaphore dbPermits = new Semaphore(50);  // Limit DB connections, not threads!

    for (Request request : requests) {
        executor.submit(() -> {
            dbPermits.acquire();
            try { return handleRequest(request); }
            finally { dbPermits.release(); }
        });
    }
}
```

---

## Quick Reference: Which Tool for Which Job

| Problem | Tool |
|---------|------|
| Simple flag (stop/ready) | `volatile boolean` |
| Counter (low contention) | `AtomicInteger` / `AtomicLong` |
| Counter (high contention) | `LongAdder` |
| Single-variable update | `AtomicReference` + CAS |
| Check-then-act | `synchronized` or `ReentrantLock` |
| Read-heavy shared data | `ReadWriteLock` or `StampedLock` |
| Thread-safe map | `ConcurrentHashMap` |
| Producer-consumer | `BlockingQueue` |
| Wait for N tasks | `CountDownLatch` |
| Reusable barrier | `CyclicBarrier` or `Phaser` |
| Resource limiting | `Semaphore` |
| Async pipeline | `CompletableFuture` |
| I/O-bound concurrency (Java 21+) | Virtual threads |
| Structured async scope (Java 21+) | `StructuredTaskScope` |

---

## Common Anti-Patterns to Avoid

1. **Synchronizing on a non-final field** — the lock object can change, defeating mutual exclusion.
2. **Synchronizing on a boxed primitive** — `synchronized(Integer.valueOf(1))` uses a cached instance shared globally.
3. **Calling `Thread.stop()` or `Thread.suspend()`** — deprecated and unsafe.
4. **Swallowing `InterruptedException`** — always restore the flag or propagate.
5. **Using `double`/`float` for money** — use `BigDecimal`.
6. **Spinning (`while (!done) {}`)** without `volatile` or sleep — wastes CPU and may never terminate.
7. **Unbounded queues in production** — leads to OOM under load.
8. **Using `ConcurrentHashMap` with external check-then-act** — use `putIfAbsent()`, `computeIfAbsent()`, etc.
9. **Creating a new thread per request** — use thread pools or virtual threads.
10. **Locking with `synchronized` in virtual thread I/O paths** — causes pinning; use `ReentrantLock`.

---

## Recommended Reading

- **[Java Concurrency in Practice](https://jcip.net/)** by Brian Goetz — the definitive book on Java concurrency.
- **[JLS Chapter 17: Threads and Locks](https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html)** — the formal specification.
- **[JEP 444: Virtual Threads](https://openjdk.org/jeps/444)** — the official JEP for virtual threads (Java 21).
- **[JEP 453: Structured Concurrency](https://openjdk.org/jeps/453)** — structured concurrency preview (Java 21+).

---

## Related Resources in This Repo

- **[TransactionValidator README](src/main/java/howToSolveCodingProblems/transactionValidator/README.md)** — Working example of AtomicReference, ReentrantLock, ConcurrentHashMap, and CAS.
- **[Account.java](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java)** — Thread-safe account demonstrating three concurrency mechanisms.
- **[TransactionValidatorTest.java](src/test/java/howToSolveCodingProblems/transactionValidator/TransactionValidatorTest.java)** — CountDownLatch patterns for testing concurrent code.
- **[JavaInterviewTips.md](JavaInterviewTips.md)** — General Java interview tips and resources.
