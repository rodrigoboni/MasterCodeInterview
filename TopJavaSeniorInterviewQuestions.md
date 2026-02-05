# Top Java Senior Interview Questions

A comprehensive Q&A guide for **senior software engineer** technical interviews covering **Java Core**, **Spring Boot**, and **Databases** with equal depth (~94 questions total).

**Java version:** Examples target **Java 17+** / **Spring Boot 3.x**. SQL examples use **PostgreSQL**-compatible syntax.

> **Companion resources:**
> - [TopJavaConcurrencyInterviewQuestions.md](TopJavaConcurrencyInterviewQuestions.md) — 73 Q&A on concurrency, JMM, threads, locks, atomics, and virtual threads.
> - [TransactionValidator](src/main/java/howToSolveCodingProblems/transactionValidator/README.md) — production-style code demonstrating design patterns, concurrency, and the Result Pattern.

---

## Table of Contents

### PART I — JAVA CORE (Q1–Q32)
1. [OOP & SOLID Principles](#1-oop--solid-principles) — Q1–Q6
2. [Collections Framework Internals](#2-collections-framework-internals) — Q7–Q13
3. [Generics & Type System](#3-generics--type-system) — Q14–Q18
4. [Exception Handling](#4-exception-handling) — Q19–Q22
5. [Streams & Functional Programming](#5-streams--functional-programming) — Q23–Q27
6. [Memory Management & Garbage Collection](#6-memory-management--garbage-collection) — Q28–Q32

### PART II — SPRING BOOT (Q33–Q65)
7. [IoC, Dependency Injection & Bean Lifecycle](#section-7-ioc-dependency-injection--bean-lifecycle) — Q33–Q38
8. [Spring Boot Auto-Configuration & Externalized Config](#section-8-spring-boot-auto-configuration--externalized-config) — Q39–Q43
9. [Spring MVC & REST APIs](#section-9-spring-mvc--rest-apis) — Q44–Q49
10. [Spring Data JPA & Persistence](#section-10-spring-data-jpa--persistence) — Q50–Q55
11. [Transaction Management](#section-11-transaction-management) — Q56–Q60
12. [Testing in Spring Boot](#section-12-testing-in-spring-boot) — Q61–Q65

### PART III — DATABASES (Q66–Q94)
13. [SQL Fundamentals & Joins](#13-sql-fundamentals--joins) — Q66–Q70
14. [Indexes & Query Optimization](#14-indexes--query-optimization) — Q71–Q76
15. [Transactions & Isolation Levels](#15-transactions--isolation-levels) — Q77–Q81
16. [Database Design & Normalization](#16-database-design--normalization) — Q82–Q85
17. [N+1 Problem & ORM Pitfalls](#17-n1-problem--orm-pitfalls) — Q86–Q89
18. [Scaling, Caching & NoSQL](#18-scaling-caching--nosql) — Q90–Q94

### Closing
- [Quick-Reference Decision Matrix](#quick-reference-decision-matrix)
- [Cross-Part Connections](#cross-part-connections)
- [Common Anti-Patterns](#common-anti-patterns-across-all-three-areas)
- [Recommended Reading](#recommended-reading)
- [Related Resources](#related-resources-in-this-repo)

---



## PART I — JAVA CORE

---

### 1. OOP & SOLID Principles

---

### **Q1: What are the four pillars of OOP? Explain each with Java examples.**

**Encapsulation** — bundling data and the methods that operate on it into a single unit (class), while restricting direct access to internal state. This protects invariants and allows the internal representation to change without breaking clients.

```java
public class BankAccount {
    private BigDecimal balance; // hidden state

    public BankAccount(BigDecimal initial) {
        if (initial.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Initial balance cannot be negative");
        this.balance = initial;
    }

    public BigDecimal getBalance() { return balance; }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Deposit must be positive");
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(balance) > 0)
            throw new InsufficientFundsException(balance, amount);
        balance = balance.subtract(amount);
    }
}
```

**Inheritance** — a mechanism where a subclass acquires the fields and methods of a superclass, enabling code reuse and establishing an **is-a** relationship. Overuse leads to fragile base-class problems; prefer composition when the relationship is **has-a**.

```java
public abstract class Notification {
    protected final String recipient;
    protected final Instant createdAt = Instant.now();

    protected Notification(String recipient) { this.recipient = recipient; }

    public abstract void send();
}

public class EmailNotification extends Notification {
    private final String subject;
    public EmailNotification(String to, String subject) { super(to); this.subject = subject; }

    @Override
    public void send() {
        System.out.printf("Sending email to %s: %s%n", recipient, subject);
    }
}
```

**Polymorphism** — the ability to treat objects of different concrete types through a common interface. In Java this manifests as **compile-time** (method overloading) and **runtime** polymorphism (method overriding / virtual dispatch).

```java
// Runtime polymorphism — client code is decoupled from concrete types
List<Notification> pending = List.of(
    new EmailNotification("a@b.com", "Welcome"),
    new SmsNotification("+155512345")
);
pending.forEach(Notification::send); // dynamic dispatch
```

**Abstraction** — exposing only the essential behavior while hiding implementation complexity. In Java, achieved via abstract classes and interfaces. Clients program to the contract, not the implementation.

```java
// The caller never knows whether this is an in-memory, Redis, or DB cache
public interface Cache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value, Duration ttl);
    void evict(K key);
}
```

---

### **Q2: Why should you "favor composition over inheritance"? Show with code.**

Inheritance creates tight **compile-time coupling** between parent and child. If the parent's implementation changes, subclasses may silently break (the **fragile base-class** problem). Composition delegates to a collaborator behind an interface, making behavior pluggable at runtime and easier to test.

**Classic problem — broken `HashSet` counter via inheritance:**

```java
// FRAGILE — addAll() internally calls add(), so count is doubled
public class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;

    @Override
    public boolean add(E e) { addCount++; return super.add(e); }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c); // calls add() internally → double-count!
    }
}
```

**Fix with composition (wrapper / decorator):**

```java
public class InstrumentedSet<E> implements Set<E> {
    private final Set<E> delegate;       // composition
    private int addCount = 0;

    public InstrumentedSet(Set<E> delegate) { this.delegate = delegate; }

    @Override
    public boolean add(E e) { addCount++; return delegate.add(e); }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return delegate.addAll(c);       // no double-count
    }

    public int getAddCount() { return addCount; }

    // remaining Set methods forwarded to delegate ...
    @Override public int size() { return delegate.size(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    // ... etc.
}
```

**When inheritance is still valid:** true **is-a** relationships with stable base classes (`AbstractList`, `AbstractMap`), and when the superclass was explicitly designed for extension (documented self-use patterns, `protected` hooks).

---

### **Q3: Explain each SOLID principle with a Java code example.**

**S — Single Responsibility Principle (SRP)**
A class should have only one reason to change. If a class handles both business logic and persistence, a database schema change forces modification even though the business rules are unchanged.

```java
// BAD — two responsibilities
public class OrderService {
    public Order createOrder(Cart cart) { /* business logic */ }
    public void saveToDatabase(Order order) { /* JDBC code */ }
}

// GOOD — separated
public class OrderService {
    private final OrderRepository repo;
    public OrderService(OrderRepository repo) { this.repo = repo; }
    public Order createOrder(Cart cart) {
        Order order = Order.from(cart);
        return repo.save(order);
    }
}
```

**O — Open/Closed Principle (OCP)**
Software entities should be open for extension but closed for modification. Use polymorphism or strategy injection so new behavior doesn't require editing existing code.

```java
// New discount types don't touch existing code
public sealed interface DiscountPolicy permits PercentageDiscount, FlatDiscount, NoDiscount {
    BigDecimal apply(BigDecimal price);
}

public record PercentageDiscount(int percent) implements DiscountPolicy {
    public BigDecimal apply(BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100 - percent))
                     .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }
}
```

**L — Liskov Substitution Principle (LSP)**
Subtypes must be substitutable for their base types without altering program correctness. The classic violation: `Square extends Rectangle` where `setWidth` silently changes height.

```java
// Correct modeling with sealed interface — no misleading inheritance
public sealed interface Shape permits Rectangle, Square, Circle {
    double area();
}
public record Rectangle(double width, double height) implements Shape {
    public double area() { return width * height; }
}
public record Square(double side) implements Shape {
    public double area() { return side * side; }
}
```

**I — Interface Segregation Principle (ISP)**
Clients should not be forced to depend on methods they don't use. Split fat interfaces into focused ones.

```java
// BAD
public interface SmartDevice {
    void print();
    void scan();
    void fax();
}

// GOOD
public interface Printer   { void print(); }
public interface Scanner   { void scan(); }
public interface FaxDevice { void fax(); }

public class BasicPrinter implements Printer {
    public void print() { /* ... */ }
    // no scan or fax obligations
}
```

**D — Dependency Inversion Principle (DIP)**
High-level modules should depend on abstractions, not on low-level modules. Both should depend on interfaces.

```java
// High-level policy depends on abstraction
public class NotificationService {
    private final MessageSender sender; // abstraction

    public NotificationService(MessageSender sender) { this.sender = sender; }

    public void notifyUser(User user, String msg) {
        sender.send(user.contact(), msg);
    }
}

public interface MessageSender {
    void send(String destination, String message);
}

// Low-level implementations
public class SmsSender implements MessageSender { /* Twilio SDK */ }
public class EmailSender implements MessageSender { /* SMTP */ }
```

---

### **Q4: Interface vs abstract class — what changed with Java 8+ default methods and Java 17 sealed interfaces?**

| Aspect | `interface` | `abstract class` |
|---|---|---|
| Instantiation | Cannot be instantiated | Cannot be instantiated |
| Constructors | No | Yes |
| State (instance fields) | Only `public static final` constants | Any fields |
| Method types | `abstract`, `default`, `static`, `private` (Java 9+) | `abstract`, concrete, `static`, `final` |
| Multiple inheritance | A class can implement many interfaces | Single class inheritance only |
| Access modifiers on methods | `public` (abstract/default), `private` (Java 9+ helpers) | Any access modifier |
| Sealed subtypes (Java 17) | `sealed interface … permits` | `sealed class … permits` |
| Lambda target | Yes (if `@FunctionalInterface` with one abstract method) | No |
| Typical use | Defining a **capability** / contract | Sharing **state + partial implementation** |

**Default methods** (Java 8) let interfaces evolve without breaking existing implementors:

```java
public interface Sortable<T extends Comparable<T>> {
    List<T> items();

    default List<T> sorted() {           // default implementation
        return items().stream().sorted().toList();
    }
}
```

**Sealed interfaces** (Java 17) restrict which classes may implement the interface, enabling exhaustive `switch` expressions:

```java
public sealed interface PaymentResult permits Approved, Declined, RequiresRetry {
    String transactionId();
}
public record Approved(String transactionId, Instant at) implements PaymentResult {}
public record Declined(String transactionId, String reason) implements PaymentResult {}
public record RequiresRetry(String transactionId, Duration backoff) implements PaymentResult {}

// Exhaustive pattern matching (Java 21 preview → stable)
public String describe(PaymentResult r) {
    return switch (r) {
        case Approved a   -> "Approved at " + a.at();
        case Declined d   -> "Declined: " + d.reason();
        case RequiresRetry rr -> "Retry after " + rr.backoff();
    };
}
```

**Rule of thumb:** start with an interface. Use an abstract class only when you need shared mutable state or constructor logic across subtypes.

---

### **Q5: Explain coupling and cohesion. How do you measure them, and why do they matter?**

**Cohesion** measures how strongly the responsibilities within a single module are related. **High cohesion** means every method and field in a class serves a single, well-defined purpose.

**Coupling** measures how much one module depends on the internals of another. **Low (loose) coupling** means modules interact through narrow, well-defined interfaces.

The goal is **high cohesion + low coupling**.

| Metric | Low (bad for cohesion / good for coupling) | High (good for cohesion / bad for coupling) |
|---|---|---|
| **Cohesion** | A `UserManager` that handles auth, email, and reports | A `PasswordEncoder` that only hashes passwords |
| **Coupling** | Classes interact only via interfaces | Class A directly reads private fields of class B via reflection |

**Measuring coupling in practice:**

- **Afferent coupling (Ca):** how many classes depend on this class (high → risky to change).
- **Efferent coupling (Ce):** how many classes this class depends on (high → fragile).
- **Instability = Ce / (Ca + Ce):** 0 = maximally stable, 1 = maximally unstable.
- Tools: **JDepend**, **ArchUnit**, **SonarQube** module-dependency graphs.

```java
// HIGH coupling — OrderService knows concrete EmailSender, JdbcRepo, PdfGenerator
public class OrderService {
    private final JdbcOrderRepository repo = new JdbcOrderRepository();
    private final SmtpEmailSender email = new SmtpEmailSender();
}

// LOW coupling — depends on abstractions, injected
public class OrderService {
    private final OrderRepository repo;
    private final NotificationSender notifier;

    public OrderService(OrderRepository repo, NotificationSender notifier) {
        this.repo = repo;
        this.notifier = notifier;
    }
}
```

**Measuring cohesion:**
- **LCOM (Lack of Cohesion of Methods):** counts method pairs that don't share instance fields. A high LCOM signals a class should be split.
- Informal check: if you can't describe a class's purpose in one sentence without "and", its cohesion is too low.

---

### **Q6: Which design patterns come up most in senior Java interviews? Show code for each.**

**Quick Reference Table:**

| Pattern | Category | When to use |
|---|---|---|
| **Strategy** | Behavioral | Swap algorithms at runtime (pricing, validation, sorting) |
| **Observer** | Behavioral | Event-driven notification (listeners, pub/sub) |
| **Factory Method** | Creational | Decouple object creation from usage |
| **Builder** | Creational | Complex object construction with many optional params |
| **Decorator** | Structural | Add behavior to objects dynamically without subclassing |
| **Result (sealed)** | Functional | Represent success/failure without exceptions for control flow |

**Strategy:**

```java
@FunctionalInterface
public interface PricingStrategy {
    BigDecimal calculate(Order order);
}

public class RegularPricing implements PricingStrategy {
    public BigDecimal calculate(Order order) { return order.subtotal(); }
}

public class HolidayPricing implements PricingStrategy {
    public BigDecimal calculate(Order order) {
        return order.subtotal().multiply(BigDecimal.valueOf(0.8));
    }
}

// Usage — strategy is injected
public class CheckoutService {
    private final PricingStrategy pricing;
    public CheckoutService(PricingStrategy pricing) { this.pricing = pricing; }
    public Invoice checkout(Order order) {
        BigDecimal total = pricing.calculate(order);
        return new Invoice(order.id(), total);
    }
}
```

**Observer:**

```java
public interface OrderEventListener {
    void onOrderPlaced(Order order);
}

public class OrderEventPublisher {
    private final List<OrderEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(OrderEventListener l)   { listeners.add(l); }
    public void unsubscribe(OrderEventListener l)  { listeners.remove(l); }

    public void publish(Order order) {
        listeners.forEach(l -> l.onOrderPlaced(order));
    }
}

// Concrete observers
public class InventoryDeductor implements OrderEventListener {
    public void onOrderPlaced(Order order) { /* reduce stock */ }
}
public class EmailConfirmation implements OrderEventListener {
    public void onOrderPlaced(Order order) { /* send email */ }
}
```

**Factory Method:**

```java
public sealed interface Notification permits Email, Sms, Push {
    void send();
}

public class NotificationFactory {
    public static Notification create(String channel, String recipient, String body) {
        return switch (channel.toLowerCase()) {
            case "email" -> new Email(recipient, body);
            case "sms"   -> new Sms(recipient, body);
            case "push"  -> new Push(recipient, body);
            default      -> throw new IllegalArgumentException("Unknown channel: " + channel);
        };
    }
}
```

**Builder:**

```java
public class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final Duration timeout;
    private final byte[] body;

    private HttpRequest(Builder b) {
        this.url     = Objects.requireNonNull(b.url);
        this.method  = b.method;
        this.headers = Map.copyOf(b.headers);
        this.timeout = b.timeout;
        this.body    = b.body;
    }

    public static Builder builder(String url) { return new Builder(url); }

    public static class Builder {
        private final String url;
        private String method = "GET";
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Duration timeout = Duration.ofSeconds(30);
        private byte[] body;

        private Builder(String url) { this.url = url; }

        public Builder method(String m)             { this.method = m;       return this; }
        public Builder header(String k, String v)   { headers.put(k, v);     return this; }
        public Builder timeout(Duration t)           { this.timeout = t;      return this; }
        public Builder body(byte[] b)                { this.body = b;         return this; }

        public HttpRequest build() { return new HttpRequest(this); }
    }
}

// Usage
var req = HttpRequest.builder("https://api.example.com/orders")
    .method("POST")
    .header("Authorization", "Bearer token")
    .timeout(Duration.ofSeconds(5))
    .body("{\"item\":1}".getBytes())
    .build();
```

**Decorator:**

```java
public interface DataSource {
    String read();
    void write(String data);
}

public class FileDataSource implements DataSource {
    private final Path path;
    public FileDataSource(Path path) { this.path = path; }
    public String read()             { return Files.readString(path); }
    public void write(String data)   { Files.writeString(path, data); }
}

// Decorator — adds compression transparently
public class CompressionDecorator implements DataSource {
    private final DataSource wrapped;
    public CompressionDecorator(DataSource wrapped) { this.wrapped = wrapped; }

    public String read()           { return decompress(wrapped.read()); }
    public void write(String data) { wrapped.write(compress(data)); }

    private String compress(String s)   { /* gzip */ return s; }
    private String decompress(String s) { /* gunzip */ return s; }
}

// Stack decorators
DataSource ds = new CompressionDecorator(
                    new EncryptionDecorator(
                        new FileDataSource(Path.of("data.txt"))));
```

**Result Pattern (sealed interface):**

This pattern models operation outcomes as explicit types rather than throwing exceptions for expected failure paths. The `ValidationResult` in [TransactionValidator](src/main/java/howToSolveCodingProblems/transactionValidator/TransactionValidator.java) is a good real-world example of this approach.

```java
public sealed interface Result<T> permits Result.Success, Result.Failure {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error, ErrorCode code) implements Result<T> {}

    default <R> Result<R> map(Function<T, R> fn) {
        return switch (this) {
            case Success<T> s -> new Success<>(fn.apply(s.value()));
            case Failure<T> f -> new Failure<>(f.error(), f.code());
        };
    }
}
```

---

### 2. Collections Framework Internals

---

### **Q7: Draw the Java Collections hierarchy. Which interfaces sit at the top?**

```
                        Iterable<E>
                            │
                        Collection<E>
                ┌───────────┼───────────────┐
                │           │               │
             List<E>      Set<E>         Queue<E>
                │           │               │
         ┌──────┤     ┌─────┤          ┌────┴────┐
         │      │     │     │          │         │
    ArrayList  LinkedList  HashSet  TreeSet  PriorityQueue  ArrayDeque
                │(also Deque)       (SortedSet)              (Deque)
                │     │
                │  LinkedHashSet
                │
                └── implements List & Deque


            Map<K,V>  (separate hierarchy — NOT Collection)
                │
        ┌───────┼───────────┐
        │       │           │
    HashMap  TreeMap   LinkedHashMap
        │   (SortedMap/
        │    NavigableMap)
   ConcurrentHashMap
```

Key points for interviews:
- **`Map`** does not extend `Collection`. It is a parallel hierarchy.
- **`LinkedList`** implements both `List` and `Deque`.
- Thread-safe alternatives live in `java.util.concurrent`: `ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedQueue`, `BlockingQueue` implementations.
- **Unmodifiable wrappers:** `Collections.unmodifiableList(...)` and the Java 10+ `List.copyOf(...)`, `List.of(...)`.

---

### **Q8: How does `HashMap` work internally? Explain buckets, hashing, treeification, load factor, and resizing.**

A `HashMap<K,V>` is backed by an array of **buckets** (`Node<K,V>[] table`). Each bucket is a linked list (or a red-black tree after a threshold).

**Insertion flow (`put(key, value)`):**

1. Compute `hash`: `(h = key.hashCode()) ^ (h >>> 16)` — the high bits are mixed into low bits to reduce collisions in small tables.
2. Determine bucket index: `index = hash & (capacity - 1)` (bitwise AND, only works because capacity is always a power of 2).
3. Walk the chain at `table[index]`:
   - If a node with the same key (by `equals()`) exists → replace value.
   - Otherwise append a new node.
4. If the chain length reaches **`TREEIFY_THRESHOLD = 8`** and the table size is ≥ 64 → convert the linked list into a **red-black tree** (`TreeNode`), reducing worst-case lookup from **O(n)** to **O(log n)**.
5. If the tree shrinks below **`UNTREEIFY_THRESHOLD = 6`** (after removals or a resize) → convert back to a linked list.

**Load factor and resizing:**

| Parameter | Default | Meaning |
|---|---|---|
| Initial capacity | 16 | Number of buckets at creation |
| Load factor | 0.75 | Ratio `size / capacity` that triggers resize |
| Resize | × 2 | Capacity doubles; all entries are rehashed |
| Treeify threshold | 8 | Chain length that triggers linked-list → tree |

When `size > capacity × loadFactor` (e.g., 12 entries in a 16-bucket map), the table **doubles** to 32 buckets and every entry is repositioned (`rehash`). This is **O(n)** but amortized O(1) per insertion.

```java
// Pre-size to avoid resizing if you know the count
Map<String, User> cache = new HashMap<>(expectedSize * 4 / 3 + 1);
// or since Java 19:
Map<String, User> cache = HashMap.newHashMap(expectedSize);
```

**Why capacity must be a power of 2:**
The bucket index is computed via `hash & (capacity - 1)` instead of `hash % capacity`. Bitwise AND is faster and produces a valid index only when capacity is a power of 2 (all lower bits are 1).

---

### **Q9: HashMap vs TreeMap vs LinkedHashMap — when do you use each?**

| Feature | `HashMap` | `TreeMap` | `LinkedHashMap` |
|---|---|---|---|
| Ordering | None (undefined) | Sorted by key (`Comparable` or `Comparator`) | Insertion order (or access order) |
| `get`/`put` time | **O(1)** amortized | **O(log n)** | **O(1)** amortized |
| Null keys | One `null` key allowed | **Not allowed** (throws `NPE` for comparisons) | One `null` key allowed |
| Interface | `Map` | `NavigableMap` / `SortedMap` | `Map` |
| Use case | General-purpose fast lookup | Range queries, sorted iteration, floor/ceiling operations | LRU cache, preserving insertion order |
| Thread-safe? | No | No | No |

**When to choose:**

```java
// HashMap — fastest for lookup/insert, order doesn't matter
Map<String, Config> configCache = new HashMap<>();

// TreeMap — need sorted keys or range queries
TreeMap<LocalDate, List<Trade>> tradesByDate = new TreeMap<>();
tradesByDate.subMap(startDate, endDate);  // range query
tradesByDate.floorEntry(targetDate);      // nearest entry ≤ target

// LinkedHashMap — preserve insertion order for deterministic iteration
Map<String, String> headers = new LinkedHashMap<>();
headers.put("Content-Type", "application/json");
headers.put("Authorization", "Bearer ...");
// iterating yields Content-Type first, then Authorization

// LinkedHashMap as LRU cache (access-order mode)
Map<String, byte[]> lruCache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
        return size() > MAX_CACHE_SIZE;
    }
};
```

---

### **Q10: ArrayList vs LinkedList — when is each faster?**

| Operation | `ArrayList` | `LinkedList` |
|---|---|---|
| Random access `get(i)` | **O(1)** — direct array index | **O(n)** — must traverse from head/tail |
| Append `add(e)` | **O(1)** amortized (occasional resize) | **O(1)** — pointer update at tail |
| Insert at index `add(i, e)` | **O(n)** — shifts elements | **O(n)** — traversal to index, then O(1) link |
| Remove by index | **O(n)** — shifts elements | **O(n)** — traversal, then O(1) unlink |
| Memory per element | ~4 bytes (object reference) | ~40 bytes (Node with prev/next pointers + padding) |
| Cache locality | **Excellent** — contiguous array | **Poor** — nodes scattered across heap |

**In practice, `ArrayList` wins almost always.** Modern CPUs are dominated by cache performance. `ArrayList`'s contiguous memory layout means sequential access hits the L1/L2 cache, while `LinkedList` causes constant cache misses chasing scattered pointers.

```java
// Almost always prefer ArrayList
List<String> items = new ArrayList<>();

// LinkedList is justified ONLY when:
// 1. You need a Deque (but ArrayDeque is usually faster anyway)
// 2. You frequently insert/remove at both ends AND iterate (rare)
Deque<Task> taskQueue = new ArrayDeque<>(); // prefer over LinkedList even for Deque
```

**Iterator invalidation:** `ArrayList` uses a **fail-fast** `modCount` check — if the list is structurally modified (add/remove) during iteration outside the iterator, it throws `ConcurrentModificationException`. `LinkedList` has the same behavior. Neither is safe for concurrent modification without external synchronization.

---

### **Q11: How does `HashSet` work internally?**

`HashSet<E>` is backed by a `HashMap<E, Object>`. Every element is stored as a **key** in the map, with a shared dummy value (`private static final Object PRESENT = new Object()`).

```java
// Simplified internal implementation
public class HashSet<E> implements Set<E> {
    private transient HashMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();
    }

    public boolean add(E e) {
        return map.put(e, PRESENT) == null; // returns true if key was absent
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    public int size() {
        return map.size();
    }
}
```

This means:
- **O(1)** average `add`, `remove`, `contains` — same as `HashMap`.
- **Uniqueness** is enforced by `equals()` and `hashCode()` — the same contract as `HashMap` keys.
- `LinkedHashSet` wraps a `LinkedHashMap` (preserves insertion order).
- `TreeSet` wraps a `TreeMap` (sorted order).

---

### **Q12: What is `ConcurrentModificationException`? Explain fail-fast iterators.**

Java's standard collections (e.g., `ArrayList`, `HashMap`) use a **`modCount`** field that tracks structural modifications (insertions, deletions). When an iterator is created, it snapshots `modCount` as `expectedModCount`. On every `next()` or `remove()` call, the iterator checks `modCount == expectedModCount`; if they differ, it throws `ConcurrentModificationException`.

```java
List<String> names = new ArrayList<>(List.of("Alice", "Bob", "Charlie"));

// WRONG — modifying the list while iterating with enhanced for-loop
for (String name : names) {
    if (name.startsWith("B")) {
        names.remove(name); // ConcurrentModificationException!
    }
}

// CORRECT — use Iterator.remove()
Iterator<String> it = names.iterator();
while (it.hasNext()) {
    if (it.next().startsWith("B")) {
        it.remove(); // safe — iterator updates modCount internally
    }
}

// CORRECT — use removeIf (Java 8+)
names.removeIf(name -> name.startsWith("B"));

// CORRECT — use CopyOnWriteArrayList for concurrent read-heavy scenarios
List<String> safeNames = new CopyOnWriteArrayList<>(names);
for (String name : safeNames) {
    safeNames.remove(name); // no exception — iterates over a snapshot
}
```

**Important nuances:**
- Fail-fast is **best-effort**, not guaranteed (the spec says "should not be depended upon for correctness").
- This is **not** about thread safety — you can get this exception in a single thread by modifying a collection inside a `for-each` loop.
- **Concurrent collections** (`ConcurrentHashMap`, `CopyOnWriteArrayList`) use **fail-safe** or **weakly consistent** iterators that never throw `ConcurrentModificationException`.

---

### **Q13: Explain the `equals()` / `hashCode()` contract. What breaks if you violate it?**

The contract (from `Object` javadoc):

1. **Consistency:** if `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` **must** be `true`.
2. The converse is not required: two objects with the same hash code are **not** necessarily equal (hash collisions are expected).
3. `equals()` must be **reflexive** (`a.equals(a)`), **symmetric** (`a.equals(b) ↔ b.equals(a)`), **transitive**, and **consistent** across multiple invocations.

**What breaks if you override `equals()` but not `hashCode()`:**

```java
public class Employee {
    private final String employeeId;
    private final String name;

    // equals based on employeeId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee e)) return false;
        return employeeId.equals(e.employeeId);
    }

    // MISSING hashCode() override!
}

var emp1 = new Employee("E001", "Alice");
var emp2 = new Employee("E001", "Alice");

emp1.equals(emp2); // true

Set<Employee> set = new HashSet<>();
set.add(emp1);
set.contains(emp2); // FALSE! — different Object.hashCode() → different bucket
```

**Correct implementation:**

```java
@Override
public int hashCode() {
    return Objects.hash(employeeId); // only fields used in equals()
}
```

**Best practice — use `record` types when possible:**

```java
public record Employee(String employeeId, String name) {
    // equals() and hashCode() are auto-generated from all components
}
```

**JPA entity caveat:** for JPA entities, **do not use the auto-generated database ID** in `equals()`/`hashCode()` if it is assigned by the database on `persist()`. Before the entity is persisted, the ID is `null`, causing objects to be "equal" to every other unpersisted entity. Instead, use a **natural key** or **business key** (e.g., `employeeId`, `isbn`). Alternatively, use a UUID assigned at construction time.

```java
@Entity
public class Product {
    @Id @GeneratedValue
    private Long id; // do NOT use in equals/hashCode

    @Column(unique = true, nullable = false)
    private String sku; // natural key — use this

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return sku.equals(p.sku);
    }

    @Override
    public int hashCode() { return sku.hashCode(); }
}
```

---

### 3. Generics & Type System

---

### **Q14: What is type erasure? What are its runtime implications?**

**Type erasure** is the process by which the Java compiler removes all generic type information after compilation. At runtime, `List<String>` and `List<Integer>` are both just `List`. This was a deliberate design choice for backward compatibility when generics were introduced in Java 5.

**What the compiler does:**

1. Replaces type parameters with their bounds (or `Object` if unbounded).
2. Inserts casts at call sites to ensure type safety.
3. Generates **bridge methods** to preserve polymorphism in overridden generic methods.

```java
// What you write:
List<String> names = new ArrayList<>();
names.add("Alice");
String name = names.get(0);

// What exists at runtime (after erasure):
List names = new ArrayList();
names.add("Alice");
String name = (String) names.get(0); // compiler-inserted cast
```

**Things you CANNOT do because of erasure:**

```java
// 1. Cannot instantiate a type parameter
public <T> T create() {
    return new T(); // COMPILE ERROR — T is erased to Object at runtime
}

// 2. Cannot use instanceof with parameterized types
if (obj instanceof List<String>) { } // COMPILE ERROR
if (obj instanceof List<?>) { }      // OK — unbounded wildcard is fine

// 3. Cannot create generic arrays
T[] array = new T[10];               // COMPILE ERROR
List<String>[] lists = new List[10]; // unchecked warning, not type-safe

// 4. Cannot overload methods that erase to the same signature
void process(List<String> strings) { }
void process(List<Integer> ints) { }  // COMPILE ERROR — both erase to List
```

**Workaround — pass a `Class<T>` token:**

```java
public <T> T create(Class<T> type) {
    return type.getDeclaredConstructor().newInstance();
}
```

---

### **Q15: Explain wildcards: `<?>`, `<? extends T>`, `<? super T>`, and the PECS rule.**

**PECS = Producer Extends, Consumer Super** (Joshua Bloch, *Effective Java*).

| Wildcard | Read (produce) | Write (consume) | Use when the generic type is a... |
|---|---|---|---|
| `<? extends T>` | Yes — read as `T` | No (except `null`) | **Producer** — you read items out |
| `<? super T>` | Only as `Object` | Yes — write `T` or subtypes | **Consumer** — you put items in |
| `<?>` | Only as `Object` | No (except `null`) | Neither — you don't care about the type |

```java
// PRODUCER EXTENDS — reading elements out of the collection
public double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number n : numbers) { // safe to read as Number
        total += n.doubleValue();
    }
    // numbers.add(1);          // COMPILE ERROR — can't write
    return total;
}

sum(List.of(1, 2, 3));          // List<Integer> — works
sum(List.of(1.5, 2.5));         // List<Double> — works
```

```java
// CONSUMER SUPER — writing elements into the collection
public void addNumbers(List<? super Integer> target) {
    target.add(1);               // safe to write Integer
    target.add(2);
    // Integer n = target.get(0); // COMPILE ERROR — can only read as Object
}

addNumbers(new ArrayList<Integer>());  // works
addNumbers(new ArrayList<Number>());   // works
addNumbers(new ArrayList<Object>());   // works
```

```java
// Classic PECS example — Collections.copy
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (int i = 0; i < src.size(); i++) {
        dest.set(i, src.get(i)); // read from producer, write to consumer
    }
}
```

**Unbounded wildcard `<?>`:** use when the method doesn't need to know the type at all:

```java
public static int countNulls(List<?> list) {
    int count = 0;
    for (Object o : list) {
        if (o == null) count++;
    }
    return count;
}
```

---

### **Q16: What are bounded type parameters? How does `<T extends Comparable<T>>` work?**

Bounded type parameters restrict which types can be used as arguments for a generic. The bound is enforced at **compile time**, and the compiler erases `T` to the bound type (not `Object`).

```java
// T must implement Comparable<T> — enables calling compareTo()
public static <T extends Comparable<T>> T max(List<T> list) {
    if (list.isEmpty()) throw new NoSuchElementException();
    T result = list.getFirst();
    for (T item : list) {
        if (item.compareTo(result) > 0) {
            result = item;
        }
    }
    return result;
}

max(List.of(3, 1, 4, 1, 5)); // works — Integer implements Comparable<Integer>
max(List.of("z", "a", "m")); // works — String implements Comparable<String>
```

**Multiple bounds** — a type parameter can extend one class and multiple interfaces (class must come first):

```java
public <T extends Number & Comparable<T> & Serializable> void process(T value) {
    double d = value.doubleValue();  // from Number
    int cmp = value.compareTo(other); // from Comparable
}
```

**Recursive type bounds** — common in builder patterns and fluent APIs:

```java
public abstract class Builder<T, B extends Builder<T, B>> {
    protected String name;

    @SuppressWarnings("unchecked")
    public B name(String name) {
        this.name = name;
        return (B) this; // return concrete builder type for fluent chaining
    }

    public abstract T build();
}

public class UserBuilder extends Builder<User, UserBuilder> {
    private String email;
    public UserBuilder email(String email) { this.email = email; return this; }

    @Override
    public User build() { return new User(name, email); }
}

// Fluent chain returns UserBuilder, not Builder
User user = new UserBuilder().name("Alice").email("a@b.com").build();
```

---

### **Q17: Explain type inference and the diamond operator.**

**Type inference** is the compiler's ability to deduce generic type arguments from context, saving you from writing them explicitly.

**Diamond operator (Java 7+):**

```java
// Before Java 7 — type arguments repeated
Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();

// Java 7+ — diamond operator infers from left-hand side
Map<String, List<Integer>> map = new HashMap<>();
```

**Improved inference in Java 8+ (target-type inference):**

```java
// Java 8 — inference works for method arguments and lambdas
List<String> names = Collections.emptyList(); // infers <String>

// Method reference inference
Comparator<String> cmp = Comparator.comparing(String::length);

// Chained generics
Map<String, List<String>> grouped = names.stream()
    .collect(Collectors.groupingBy(s -> s.substring(0, 1))); // all types inferred
```

**`var` (Java 10+) — local-variable type inference:**

```java
var names = List.of("Alice", "Bob"); // inferred as List<String>
var map = new HashMap<String, Integer>(); // inferred as HashMap<String, Integer>

// Careful — var infers the concrete type, not the interface
var list = new ArrayList<String>(); // type is ArrayList<String>, not List<String>
```

**Where inference does NOT work (and you must be explicit):**

```java
// 1. Diamond with anonymous inner class (before Java 9)
// 2. When the compiler can't determine the type from context
var x = List.of(); // inferred as List<Object> — may not be what you want
// 3. Fields, method parameters, and return types still require explicit types
```

---

### **Q18: What are common generic pitfalls in Java?**

**1. Raw types — bypass all generic type safety:**

```java
List rawList = new ArrayList();     // raw type — no compile-time checks
rawList.add(42);
rawList.add("hello");
String s = (String) rawList.get(0); // ClassCastException at runtime!

// ALWAYS use parameterized types
List<String> safeList = new ArrayList<>();
```

**2. Heap pollution — parameterized varargs:**

```java
// Heap pollution occurs when a variable of a parameterized type
// refers to an object that is not of that type
@SafeVarargs // suppresses the warning — only use when truly safe
static <T> List<T> listOf(T... elements) {
    return List.of(elements);
}

// DANGEROUS — don't do this
@SafeVarargs
static <T> T[] toArray(T... args) {
    return args; // the runtime array type may not match T[]
}

String[] result = toArray("a", "b"); // ClassCastException — args is Object[] at runtime
```

**3. Bridge methods — compiler-generated methods to preserve polymorphism:**

```java
public interface Processor<T> {
    void process(T item);
}

public class StringProcessor implements Processor<String> {
    @Override
    public void process(String item) { /* ... */ }
}

// After erasure, the interface has: void process(Object item)
// The compiler generates a bridge method in StringProcessor:
//   public void process(Object item) { process((String) item); }
// This bridge method is visible via reflection and can cause surprises
```

**4. Cannot use primitives as type arguments:**

```java
List<int> nums = new ArrayList<>();  // COMPILE ERROR
List<Integer> nums = new ArrayList<>(); // must use wrapper — causes autoboxing overhead

// For performance-critical code, consider:
// - Eclipse Collections IntList, IntObjectMap
// - JDK arrays (int[])
// - Valhalla / primitive classes (future JDK)
```

**5. Type erasure prevents generic exception catching:**

```java
// COMPILE ERROR — cannot catch a parameterized exception type
try { /* ... */ }
catch (GenericException<String> e) { } // illegal

// Workaround — use non-generic exception types or bounded type params
class DomainException extends RuntimeException {
    private final ErrorCode code; // use a non-generic error enum instead
}
```

---

### 4. Exception Handling

---

### **Q19: Explain checked vs unchecked exceptions. When should you use each?**

```
                       Throwable
                      /         \
                 Error        Exception
              (unchecked)    /          \
                          RuntimeException   (checked exceptions)
                          (unchecked)      IOException, SQLException, etc.
```

| Aspect | Checked | Unchecked (`RuntimeException`) |
|---|---|---|
| Must declare in `throws`? | Yes | No |
| Compiler enforcement | Caller must handle or propagate | No enforcement |
| Examples | `IOException`, `SQLException`, `ParseException` | `NullPointerException`, `IllegalArgumentException`, `IllegalStateException` |
| Recoverable? | Usually yes — external failures | Usually no — programming errors |
| Use when... | Caller can reasonably recover (retry, fallback) | Precondition violation, bug, unrecoverable |

**Common exceptions to know:**

| Exception | Type | When thrown |
|---|---|---|
| `NullPointerException` | Unchecked | Dereferencing `null` |
| `IllegalArgumentException` | Unchecked | Method receives invalid argument |
| `IllegalStateException` | Unchecked | Method invoked at wrong time (e.g., iterator past end) |
| `UnsupportedOperationException` | Unchecked | Operation not supported (e.g., `List.of().add(x)`) |
| `IndexOutOfBoundsException` | Unchecked | Array/list index out of range |
| `ClassCastException` | Unchecked | Invalid cast |
| `IOException` | Checked | I/O failure (file, network) |
| `SQLException` | Checked | Database access error |
| `InterruptedException` | Checked | Thread interrupted while waiting |
| `StackOverflowError` | Error | Infinite recursion / excessive stack depth |
| `OutOfMemoryError` | Error | JVM exhausted heap space |

**Modern preference:** many frameworks (Spring, Jakarta EE) favor **unchecked exceptions** for most business errors because checked exceptions clutter method signatures and leak implementation details through layers. Reserve checked exceptions for cases where the immediate caller is expected to handle the condition.

---

### **Q20: Explain try-with-resources and suppressed exceptions.**

**`try-with-resources`** (Java 7+) automatically closes any resource that implements `AutoCloseable` at the end of the `try` block, in **reverse declaration order**.

```java
// Resources are closed automatically, even if an exception is thrown
try (var conn = dataSource.getConnection();
     var ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");) {
    ps.setLong(1, userId);
    try (var rs = ps.executeQuery()) {
        if (rs.next()) {
            return mapRow(rs);
        }
    }
}
// conn, ps, and rs are all closed here — no finally block needed
```

**Suppressed exceptions:** if both the `try` body and the `close()` method throw, the exception from the `try` body is the **primary** exception, and the `close()` exception is **suppressed** (attached to it).

```java
public class ProblematicResource implements AutoCloseable {
    public void doWork()  { throw new RuntimeException("work failed"); }
    public void close()   { throw new RuntimeException("close failed"); }
}

try (var r = new ProblematicResource()) {
    r.doWork(); // throws "work failed"
} // close() throws "close failed" — this becomes a suppressed exception

// Output:
// RuntimeException: work failed
//   Suppressed: RuntimeException: close failed
```

```java
// Accessing suppressed exceptions
try {
    // ...
} catch (Exception e) {
    for (Throwable suppressed : e.getSuppressed()) {
        log.warn("Suppressed: {}", suppressed.getMessage());
    }
    throw e;
}
```

**Custom AutoCloseable:**

```java
public class ManagedMetric implements AutoCloseable {
    private final Timer.Context timerContext;

    public ManagedMetric(Timer timer) {
        this.timerContext = timer.time(); // start timing
    }

    @Override
    public void close() {
        timerContext.stop(); // stop timing — always runs
    }
}

// Usage
try (var metric = new ManagedMetric(requestTimer)) {
    processRequest(); // metric.close() records elapsed time
}
```

---

### **Q21: What are exception handling best practices?**

**1. Catch the most specific exception, not `Exception` or `Throwable`:**

```java
// BAD — catches everything, including programming errors
try { parseAndStore(data); }
catch (Exception e) { log.error("Error", e); }

// GOOD — handle each failure mode appropriately
try { parseAndStore(data); }
catch (JsonParseException e) { return Response.badRequest("Invalid JSON"); }
catch (DataAccessException e) { throw new ServiceException("DB write failed", e); }
```

**2. Never swallow exceptions silently:**

```java
// TERRIBLE — silently loses error information
try { riskyOperation(); }
catch (IOException e) { /* empty */ }

// At minimum, log it
catch (IOException e) { log.warn("Non-critical I/O issue, continuing", e); }
```

**3. Preserve the original cause with exception chaining:**

```java
catch (SQLException e) {
    throw new RepositoryException("Failed to save order " + orderId, e); // chained
}
```

**4. Fail fast — validate early, before doing work:**

```java
public void transferMoney(Account from, Account to, BigDecimal amount) {
    Objects.requireNonNull(from, "Source account must not be null");
    Objects.requireNonNull(to, "Target account must not be null");
    if (amount.compareTo(BigDecimal.ZERO) <= 0)
        throw new IllegalArgumentException("Amount must be positive: " + amount);
    // now do the actual transfer
}
```

**5. Don't use exceptions for control flow:**

```java
// BAD — using exception for a normal condition
try {
    int value = Integer.parseInt(input);
} catch (NumberFormatException e) {
    value = defaultValue;
}

// BETTER — check first, or use Optional-returning methods
OptionalInt parsed = tryParseInt(input);
int value = parsed.orElse(defaultValue);
```

**6. Clean up resources in `finally` or use try-with-resources:**

```java
// Never leave resources dangling on the exception path
try (var lock = acquireDistributedLock(resourceId)) {
    mutateResource();
} // lock is released even on exception
```

**7. Log at the boundary, throw at the origin:**

```java
// In service layer — throw, don't log + throw (avoids duplicate logs)
throw new OrderNotFoundException(orderId);

// At API boundary (controller) — log + return error response
catch (OrderNotFoundException e) {
    log.warn("Order not found: {}", e.getOrderId());
    return ResponseEntity.notFound().build();
}
```

---

### **Q22: When should you create custom exceptions vs using standard ones?**

**Use standard exceptions when they precisely fit:**

| Standard Exception | Use for |
|---|---|
| `IllegalArgumentException` | Invalid method parameter value |
| `IllegalStateException` | Method called in wrong state |
| `NullPointerException` | Unexpected null (or use `Objects.requireNonNull`) |
| `UnsupportedOperationException` | Unimplemented or disabled feature |
| `NoSuchElementException` | Iterator/stream has no more elements |
| `ConcurrentModificationException` | Concurrent structural modification |

**Create custom exceptions when:**

1. You need domain-specific context (error codes, affected entity IDs).
2. You want to catch a category of business errors distinctly.
3. You need to carry structured data for API error responses.

```java
// Domain exception with context
public class InsufficientFundsException extends RuntimeException {
    private final String accountId;
    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientFundsException(String accountId, BigDecimal requested, BigDecimal available) {
        super("Account %s: requested %s but only %s available"
              .formatted(accountId, requested, available));
        this.accountId = accountId;
        this.requested = requested;
        this.available = available;
    }

    public String getAccountId() { return accountId; }
    public BigDecimal getRequested() { return requested; }
    public BigDecimal getAvailable() { return available; }
}
```

**Exception hierarchy for a service:**

```java
// Base exception for the order domain
public abstract sealed class OrderException extends RuntimeException
        permits OrderNotFoundException, OrderAlreadyCancelledException, InvalidOrderStateException {
    private final String orderId;
    protected OrderException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
    }
    public String getOrderId() { return orderId; }
}

public final class OrderNotFoundException extends OrderException {
    public OrderNotFoundException(String orderId) {
        super(orderId, "Order not found: " + orderId);
    }
}
```

**Rule of thumb:** if you find yourself writing `catch (IllegalArgumentException e)` and then checking the message string to decide what to do, it's time for a custom exception.

---

### 5. Streams & Functional Programming

---

### **Q23: Explain intermediate vs terminal operations, lazy evaluation, and short-circuiting.**

Streams use a **pipeline** model: a **source** → zero or more **intermediate** operations → one **terminal** operation.

**Intermediate operations** are **lazy** — they don't execute until a terminal operation triggers the pipeline. They return a new `Stream`.

**Terminal operations** are **eager** — they consume the stream and produce a result (or side effect). After a terminal operation, the stream is closed.

```java
List<String> result = orders.stream()         // source
    .filter(o -> o.total().compareTo(threshold) > 0) // intermediate (lazy)
    .map(Order::customerName)                        // intermediate (lazy)
    .distinct()                                      // intermediate (lazy/stateful)
    .sorted()                                        // intermediate (lazy/stateful)
    .limit(10)                                       // intermediate (short-circuit)
    .toList();                                       // terminal (triggers execution)
```

**Lazy evaluation means fusion:** the stream does not create intermediate collections. Each element passes through the full pipeline before the next element is pulled from the source.

```java
// Demonstrating laziness — only processes elements until limit is reached
List.of("alpha", "bravo", "charlie", "delta", "echo")
    .stream()
    .peek(s -> System.out.println("filter: " + s)) // for debugging only
    .filter(s -> s.length() > 4)
    .peek(s -> System.out.println("map: " + s))
    .map(String::toUpperCase)
    .limit(2)   // short-circuiting — stops after finding 2 matches
    .toList();

// Output (note: "echo" is never processed):
// filter: alpha → map: alpha
// filter: bravo → map: bravo
// filter: charlie  (limit reached, pipeline stops)
```

**Short-circuiting operations:**

| Type | Operations |
|---|---|
| Intermediate | `limit(n)`, `takeWhile(predicate)` |
| Terminal | `findFirst()`, `findAny()`, `anyMatch()`, `allMatch()`, `noneMatch()` |

**Stateful vs stateless intermediate operations:**

| Stateless (per-element) | Stateful (need to see all/some elements) |
|---|---|
| `filter`, `map`, `flatMap`, `peek` | `sorted`, `distinct`, `limit`, `skip` |

Stateful operations may buffer all elements (e.g., `sorted`) which negates some benefits of laziness for large streams.

---

### **Q24: List the core functional interfaces and explain when to use each.**

All reside in `java.util.function`:

| Interface | Method | Signature | Use case |
|---|---|---|---|
| `Function<T,R>` | `apply(T)` → `R` | `T → R` | Transform / map one value to another |
| `BiFunction<T,U,R>` | `apply(T,U)` → `R` | `(T, U) → R` | Combine two values into a result |
| `Predicate<T>` | `test(T)` → `boolean` | `T → boolean` | Filter / conditional check |
| `Consumer<T>` | `accept(T)` → `void` | `T → void` | Side effect (logging, saving) |
| `Supplier<T>` | `get()` → `T` | `() → T` | Lazy factory, deferred computation |
| `UnaryOperator<T>` | `apply(T)` → `T` | `T → T` | Transform value, same type in/out |
| `BinaryOperator<T>` | `apply(T,T)` → `T` | `(T, T) → T` | Reduce two values of same type |

```java
// Function — transform
Function<String, Integer> length = String::length;
Function<String, String> toUpper = String::toUpperCase;
Function<String, Integer> composed = toUpper.andThen(length); // chain

// Predicate — filter
Predicate<Order> highValue = o -> o.total().compareTo(BigDecimal.valueOf(1000)) > 0;
Predicate<Order> recent = o -> o.createdAt().isAfter(Instant.now().minus(Duration.ofDays(7)));
Predicate<Order> combined = highValue.and(recent); // compose predicates

// Consumer — side effect
Consumer<Order> logOrder = o -> log.info("Processing order {}", o.id());
Consumer<Order> notifyCustomer = o -> emailService.send(o.customerEmail(), "Order received");
Consumer<Order> both = logOrder.andThen(notifyCustomer);

// Supplier — deferred creation
Supplier<Connection> connFactory = () -> dataSource.getConnection();
Supplier<UUID> idGenerator = UUID::randomUUID;

// BinaryOperator — reduce
BinaryOperator<BigDecimal> sum = BigDecimal::add;
orders.stream().map(Order::total).reduce(BigDecimal.ZERO, sum);
```

---

### **Q25: What are the most useful Collectors? Show examples including a custom collector.**

```java
List<Order> orders = fetchOrders();

// toList() — since Java 16 (unmodifiable)
List<String> names = orders.stream().map(Order::customerName).toList();

// toCollection — when you need a specific collection type
TreeSet<String> sorted = orders.stream()
    .map(Order::customerName)
    .collect(Collectors.toCollection(TreeSet::new));

// toMap — key extractor, value extractor (throws on duplicate keys by default)
Map<String, Order> byId = orders.stream()
    .collect(Collectors.toMap(Order::id, Function.identity()));

// toMap with merge function — handles duplicate keys
Map<String, BigDecimal> totalByCustomer = orders.stream()
    .collect(Collectors.toMap(Order::customerName, Order::total, BigDecimal::add));

// groupingBy — group elements by classifier
Map<OrderStatus, List<Order>> byStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status));

// groupingBy with downstream collector
Map<OrderStatus, Long> countByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status, Collectors.counting()));

Map<String, BigDecimal> revenueByRegion = orders.stream()
    .collect(Collectors.groupingBy(
        Order::region,
        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add)
    ));

// partitioningBy — split into true/false groups
Map<Boolean, List<Order>> partitioned = orders.stream()
    .collect(Collectors.partitioningBy(o -> o.total().compareTo(BigDecimal.valueOf(100)) > 0));
List<Order> highValue = partitioned.get(true);
List<Order> lowValue  = partitioned.get(false);

// joining
String csv = orders.stream()
    .map(Order::id)
    .collect(Collectors.joining(", ", "[", "]")); // [ORD-1, ORD-2, ORD-3]

// teeing (Java 12+) — feed stream into two collectors and merge results
record Stats(long count, BigDecimal total) {}
Stats stats = orders.stream()
    .collect(Collectors.teeing(
        Collectors.counting(),
        Collectors.reducing(BigDecimal.ZERO, Order::total, BigDecimal::add),
        Stats::new
    ));
```

**Custom collector — collect into a comma-separated string with Oxford comma:**

```java
Collector<CharSequence, ?, String> oxfordComma = Collector.of(
    ArrayList::new,                          // supplier
    ArrayList::add,                          // accumulator
    (a, b) -> { a.addAll(b); return a; },   // combiner (for parallel)
    list -> {                                // finisher
        return switch (list.size()) {
            case 0 -> "";
            case 1 -> list.get(0).toString();
            case 2 -> list.get(0) + " and " + list.get(1);
            default -> {
                var sb = new StringBuilder();
                for (int i = 0; i < list.size() - 1; i++) {
                    sb.append(list.get(i)).append(", ");
                }
                sb.append("and ").append(list.getLast());
                yield sb.toString();
            }
        };
    }
);

String result = Stream.of("Alice", "Bob", "Charlie")
    .collect(oxfordComma); // "Alice, Bob, and Charlie"
```

---

### **Q26: What are `Optional` best practices and anti-patterns?**

`Optional` was designed as a **return type** to explicitly signal that a method may not produce a value. It should not be used as a field type, method parameter, or in collections.

**Best practices:**

```java
// 1. Use as a return type to signal possible absence
public Optional<User> findByEmail(String email) {
    return Optional.ofNullable(userRepository.queryByEmail(email));
}

// 2. Use map/flatMap for transformations
Optional<String> city = findByEmail("a@b.com")
    .map(User::address)
    .map(Address::city);

// 3. Provide a default with orElse / orElseGet
String name = findByEmail(email)
    .map(User::name)
    .orElse("Anonymous");

// 4. Use orElseGet for expensive defaults (lazy evaluation)
User user = findById(id)
    .orElseGet(() -> createDefaultUser(id)); // only invoked if empty

// 5. Throw a meaningful exception on absence
User user = findById(id)
    .orElseThrow(() -> new UserNotFoundException(id));

// 6. Use ifPresentOrElse (Java 9+)
findById(id).ifPresentOrElse(
    user -> log.info("Found user: {}", user.name()),
    () -> log.warn("User {} not found", id)
);

// 7. Stream interop (Java 9+)
List<User> activeUsers = userIds.stream()
    .map(this::findById)
    .flatMap(Optional::stream) // unwrap present values, skip empties
    .filter(User::isActive)
    .toList();
```

**Anti-patterns to avoid:**

```java
// BAD — calling get() without checking (defeats the purpose)
User user = findById(id).get(); // throws NoSuchElementException if empty

// BAD — isPresent() + get() instead of map/orElse
if (opt.isPresent()) {
    return opt.get().name(); // just use opt.map(User::name)
}

// BAD — Optional as method parameter
public void sendEmail(Optional<String> cc) { } // just use @Nullable or overloads

// BAD — Optional as field
public class Order {
    private Optional<Discount> discount; // use @Nullable Discount instead
}

// BAD — Optional in collections
List<Optional<User>> users; // use List<User> and filter nulls

// BAD — Optional.of() with nullable value
Optional.of(possiblyNull); // NPE! Use Optional.ofNullable()

// BAD — orElse with expensive computation
user.orElse(createExpensiveDefault()); // always evaluates!
user.orElseGet(() -> createExpensiveDefault()); // lazy — only when empty
```

---

### **Q27: When are streams NOT appropriate? When should you use a loop instead?**

Streams are not universally better than loops. Here are situations where a traditional loop is preferable:

**1. When you need to modify local variables (streams require effectively final):**

```java
// Cannot do this with a stream — counter must be effectively final
int count = 0;
for (String s : strings) {
    if (s.length() > 5) count++;
    if (count >= 3) break; // early exit with side-effect state
}
```

**2. When performance is critical and the overhead matters:**

```java
// For tight numeric loops, primitive arrays are faster than boxed streams
int sum = 0;
for (int i = 0; i < array.length; i++) {
    sum += array[i]; // no boxing, no virtual dispatch, trivially JIT-optimized
}
// vs.
int sum = IntStream.of(array).sum(); // small overhead, usually negligible
```

**3. When you need checked exception handling:**

```java
// Streams don't play well with checked exceptions
files.stream()
    .map(f -> {
        try { return Files.readString(f); }      // ugly try-catch in lambda
        catch (IOException e) { throw new UncheckedIOException(e); }
    })
    .toList();

// Cleaner with a loop
List<String> contents = new ArrayList<>();
for (Path f : files) {
    contents.add(Files.readString(f)); // IOException propagates naturally
}
```

**4. When you need index-based access or the previous/next element:**

```java
// Sliding-window comparison — awkward with streams
for (int i = 1; i < prices.size(); i++) {
    if (prices.get(i) > prices.get(i - 1) * 1.1) {
        alerts.add(new PriceSpike(i, prices.get(i)));
    }
}
```

**5. When readability suffers — deeply nested stream operations:**

```java
// If a stream pipeline requires more than ~5 chained operations,
// or nested collectors, consider extracting to helper methods or using loops
// BAD — unreadable
var result = orders.stream()
    .collect(groupingBy(Order::region,
        filtering(o -> o.status() == COMPLETED,
            collectingAndThen(
                toMap(Order::id, Order::total, BigDecimal::add),
                m -> m.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .limit(5)
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new))))));
```

**6. When you need early return or complex control flow (break, continue, return):**

Streams support `takeWhile`/`dropWhile` (Java 9+) and short-circuiting terminals, but complex multi-condition early exits are cleaner in loops.

**Rule of thumb:** use streams for declarative data transformation pipelines. Use loops when you need mutable state, complex control flow, or when the stream version is harder to read than the loop.

---

### 6. Memory Management & Garbage Collection

---

### **Q28: Describe the JVM memory areas.**

| Memory Area | Stores | Thread-shared? | Size control |
|---|---|---|---|
| **Heap** | Objects, arrays, class instances | Yes | `-Xms` (initial), `-Xmx` (max) |
| **Stack** (per thread) | Local variables, method call frames, partial results | No (per-thread) | `-Xss` (stack size per thread) |
| **Metaspace** (off-heap, since Java 8) | Class metadata, method bytecode, constant pool | Yes | `-XX:MaxMetaspaceSize` |
| **Code Cache** | JIT-compiled native code | Yes | `-XX:ReservedCodeCacheSize` |
| **Direct Memory** (off-heap) | NIO `ByteBuffer.allocateDirect()` | Yes | `-XX:MaxDirectMemorySize` |
| **Thread-local allocation buffer (TLAB)** | Per-thread allocation regions within Eden | No (per-thread) | Managed by JVM automatically |

**Heap structure (generational):**

```
Heap
├── Young Generation
│   ├── Eden Space          — new objects allocated here
│   ├── Survivor 0 (S0)    — objects that survived 1+ minor GC
│   └── Survivor 1 (S1)    — alternates with S0
└── Old Generation (Tenured) — long-lived objects promoted from young gen
```

**Stack frame contents:**
Each method invocation creates a new frame containing:
- Local variable array (primitives stored directly, objects as references)
- Operand stack (for bytecode computation)
- Reference to the runtime constant pool of the method's class

**`StackOverflowError`** happens when the stack depth exceeds `-Xss` (default ~512K–1M depending on JVM and platform). Typically caused by infinite recursion.

**`OutOfMemoryError`** variants:
- `Java heap space` — heap exhausted → increase `-Xmx` or fix memory leak.
- `Metaspace` — too many loaded classes (common in hot-redeploying app servers).
- `unable to create native thread` — OS thread limit reached.
- `Direct buffer memory` — too many direct `ByteBuffer` allocations.

---

### **Q29: How does garbage collection work? Explain generational GC and GC roots.**

**GC roots** — the starting points for reachability analysis. An object is **eligible for GC** if it is not reachable from any root:

- **Local variables** on active thread stacks
- **Static fields** of loaded classes
- **Active threads** themselves
- **JNI references** (native code holding Java references)
- **Synchronization monitors** (objects used in `synchronized`)

**Generational hypothesis:** most objects die young. Dividing the heap into generations exploits this:

1. **Minor GC (Young GC):** collects Eden + one Survivor space. Fast (milliseconds). Uses **copying collection** — live objects are copied to the other Survivor space; everything left behind is garbage.
2. Objects that survive enough minor GCs (default threshold ~15) are **promoted (tenured)** to Old Generation.
3. **Major GC (Old GC / Full GC):** collects Old Generation. Slower, involves compaction. May cause longer pauses.

```
New object → Eden
   ↓ (minor GC)
Survivor 0 ↔ Survivor 1  (ping-pong)
   ↓ (age threshold reached)
Old Generation
   ↓ (major GC when full)
Reclaimed
```

**Mark-and-sweep (simplified):**
1. **Mark phase:** starting from GC roots, traverse the object graph and mark every reachable object.
2. **Sweep phase:** scan the heap and reclaim memory from unmarked objects.
3. **Compact phase (optional):** slide live objects together to eliminate fragmentation.

**Important concepts:**
- **Stop-the-world (STW):** GC pauses where application threads are suspended. All collectors have some STW time; modern collectors (G1, ZGC) minimize it.
- **Safepoints:** points in code where threads can be safely paused for GC (method returns, loop back-edges, allocation sites).
- **Card table / remembered set:** data structures that track cross-generational references so that minor GC doesn't need to scan the entire old generation.

---

### **Q30: Compare GC algorithms available in modern JVMs.**

| GC | Flag | Pause goal | Heap size sweet spot | Key characteristic |
|---|---|---|---|---|
| **Serial** | `-XX:+UseSerialGC` | N/A (single-threaded STW) | Small heaps (<100 MB) | Single GC thread, simple, lowest overhead |
| **Parallel (Throughput)** | `-XX:+UseParallelGC` | Maximize throughput | Medium (1–4 GB) | Multiple GC threads, STW for all phases |
| **G1** (default since JDK 9) | `-XX:+UseG1GC` | `-XX:MaxGCPauseMillis=200` (default) | Medium–large (4–64 GB) | Region-based, incremental compaction, predictable pauses |
| **ZGC** | `-XX:+UseZGC` | Sub-millisecond pauses | Large (8 GB–16 TB) | Colored pointers, load barriers, concurrent compaction |
| **Shenandoah** | `-XX:+UseShenandoahGC` | Low pause | Large (similar to ZGC) | Concurrent compaction via Brooks pointers |

**G1 GC details (most common in production):**

- Divides heap into ~2048 equal-sized **regions** (not contiguous generations).
- Each region can be Eden, Survivor, Old, or Humongous (objects > 50% of region size).
- **Mixed collections:** G1 can collect young regions + a subset of old regions together, avoiding full-heap pauses.
- `-XX:MaxGCPauseMillis=200` — G1 adaptively selects how many regions to collect to meet this target.

**ZGC details (for ultra-low-latency):**

- Pause times are typically **< 1 ms** regardless of heap size.
- Uses **colored pointers** (metadata stored in pointer bits) and **load barriers** to do almost all work concurrently.
- Generational ZGC (JDK 21+) — adds generational collection to ZGC for better throughput.
- Ideal for heaps of 8 GB+ where consistent latency matters more than raw throughput.

```bash
# Typical production JVM flags for G1
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+ParallelRefProcEnabled \
     -Xlog:gc*:file=gc.log:time,tags:filecount=5,filesize=10M \
     -jar app.jar

# For low-latency services
java -Xms8g -Xmx8g \
     -XX:+UseZGC \
     -XX:+ZGenerational \
     -jar app.jar
```

---

### **Q31: What causes memory leaks in Java? Give concrete examples.**

Despite garbage collection, Java programs can leak memory when objects remain **reachable but unused** — the GC cannot reclaim them because something still holds a reference.

**1. Static collections that grow unboundedly:**

```java
public class EventBus {
    // Static map never releases entries — memory grows forever
    private static final Map<String, List<Listener>> listeners = new HashMap<>();

    public static void register(String event, Listener l) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(l);
    }
    // No unregister method — listeners accumulate indefinitely
}
```

**2. Listeners / callbacks not deregistered:**

```java
// Observer registers but never unregisters
public class PriceTracker implements PriceChangeListener {
    public PriceTracker(PriceService service) {
        service.addListener(this); // PriceService holds a reference to PriceTracker
    }
    // If PriceTracker should be GC'd but PriceService is long-lived → leak
}

// Fix — use WeakReference or explicit deregistration
public void close() {
    priceService.removeListener(this);
}
```

**3. Non-static inner classes hold a reference to the enclosing instance:**

```java
public class Outer {
    private byte[] heavyData = new byte[10_000_000]; // 10 MB

    public Runnable createTask() {
        // Anonymous inner class holds implicit reference to Outer.this
        return new Runnable() {
            @Override public void run() { System.out.println("task"); }
        };
        // Even though task doesn't use heavyData, it prevents Outer from being GC'd
    }

    // Fix — use a static nested class or lambda (lambdas don't capture 'this' unless needed)
    public Runnable createTaskFixed() {
        return () -> System.out.println("task"); // no capture of Outer.this
    }
}
```

**4. ThreadLocal not cleaned up:**

```java
private static final ThreadLocal<UserContext> context = new ThreadLocal<>();

public void handleRequest(Request req) {
    context.set(new UserContext(req.userId()));
    try {
        processRequest(req);
    } finally {
        context.remove(); // CRITICAL — without this, thread pool threads retain the value
    }
}
// In a thread pool, threads are reused — old ThreadLocal values persist
// and prevent UserContext (and everything it references) from being GC'd
```

**5. Unclosed resources / connection pool leaks:**

Database connections, input streams, and other resources that are not closed will leak the underlying OS resource and potentially heap memory. This is especially dangerous with connection pools — a leaked connection is never returned to the pool, eventually exhausting it and causing the application to hang (see [Section 18](#18-connection-pools--datasources)).

```java
// BAD — connection leaked if exception occurs before close
Connection conn = dataSource.getConnection();
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT ...");
// exception here → conn never returned to pool

// GOOD — try-with-resources guarantees return to pool
try (var conn = dataSource.getConnection();
     var stmt = conn.createStatement();
     var rs = stmt.executeQuery("SELECT ...")) {
    // process results
}
```

**Detecting leaks:** use heap dump analysis tools (**Eclipse MAT**, **VisualVM**, **JFR + JMC**), or enable `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof`.

---

### **Q32: Explain the String pool, String immutability, and `StringBuilder` vs concatenation.**

**String immutability:** `String` objects in Java are immutable — once created, their content cannot be changed. Every "modification" (e.g., `concat`, `substring`, `replace`) creates a **new** `String` object.

**Why immutable?**
- **Thread safety:** immutable objects are inherently thread-safe — no synchronization needed.
- **Hash caching:** `String` caches its `hashCode()` (computed once, stored in a field). This makes `HashMap` lookups with `String` keys very fast.
- **Security:** strings used as class names, URLs, file paths, and credentials cannot be altered after creation.
- **String pool:** only works because strings are immutable — sharing is safe.

**String pool (intern pool):**

The JVM maintains a pool of unique `String` instances. String literals and compile-time constant expressions are automatically interned.

```java
String a = "hello";         // stored in pool
String b = "hello";         // reuses same object from pool
String c = new String("hello"); // creates a NEW object on the heap (not in pool)

System.out.println(a == b);  // true  — same pooled instance
System.out.println(a == c);  // false — different objects
System.out.println(a.equals(c)); // true — same content

// Manual interning
String d = c.intern();      // returns the pooled instance
System.out.println(a == d);  // true
```

Since Java 7, the string pool lives in the **heap** (not PermGen), so it benefits from regular garbage collection. Controlled by `-XX:StringTableSize` (hash table buckets, default 65536).

**StringBuilder vs String concatenation:**

```java
// BAD — in a loop, + creates a new String each iteration → O(n²)
String result = "";
for (int i = 0; i < 100_000; i++) {
    result += i + ","; // new StringBuilder + toString() each iteration!
}

// GOOD — single StringBuilder, O(n)
var sb = new StringBuilder(1_000_000); // pre-size if you know approximate length
for (int i = 0; i < 100_000; i++) {
    sb.append(i).append(',');
}
String result = sb.toString();
```

**Modern compiler optimizations (Java 9+ `invokedynamic` string concatenation):**

```java
// Simple concatenation outside a loop — the compiler handles this efficiently
String msg = "User " + name + " logged in at " + timestamp;
// JDK 9+ compiles this to an invokedynamic call that is as fast as StringBuilder
// No need to manually use StringBuilder for single-expression concatenation
```

| Scenario | Recommendation |
|---|---|
| Single-line concatenation | `+` operator (compiler optimizes via `invokedynamic`) |
| Loop concatenation | `StringBuilder` (explicit control, pre-size with `capacity`) |
| Thread-safe building (rare) | `StringBuffer` (synchronized — slower, rarely needed) |
| Joining a collection | `String.join()` or `Collectors.joining()` |
| Large template | `String.formatted()` or `MessageFormat` |

```java
// String.join — clean alternative for collections
String csv = String.join(", ", listOfStrings);

// Collectors.joining — for stream pipelines
String csv = stream.map(Object::toString)
    .collect(Collectors.joining(", ", "[", "]"));

// Text blocks (Java 15+) — multi-line strings
String json = """
    {
        "name": "%s",
        "age": %d
    }
    """.formatted(name, age);
```

## PART II — SPRING BOOT

### Section 7: IoC, Dependency Injection & Bean Lifecycle

---

### **Q33: What is Inversion of Control (IoC) and what are the types of Dependency Injection? Why is constructor injection preferred?**

**Inversion of Control (IoC)** is a design principle in which the control of object creation and lifecycle management is transferred from the application code to a framework or container. In Spring, the **IoC container** (`ApplicationContext`) is responsible for instantiating, configuring, and assembling beans.

**Dependency Injection (DI)** is the primary mechanism through which IoC is achieved. Instead of a class creating its own dependencies, the container *injects* them.

#### DI Types Comparison

| Aspect | **Constructor Injection** | **Setter Injection** | **Field Injection** |
|---|---|---|---|
| **Annotation** | `@Autowired` on constructor (optional since Spring 4.3 for single-constructor classes) | `@Autowired` on setter method | `@Autowired` on field directly |
| **Immutability** | Fields can be `final` | Fields must be mutable | Fields must be mutable |
| **Required dependencies** | Enforced at compile time -- object cannot be created without them | Can produce partially initialized objects | Can produce partially initialized objects |
| **Testability** | Easy -- pass mocks via constructor in plain unit tests | Possible but verbose -- need to call setters | Requires reflection or `@InjectMocks` -- leaks framework into tests |
| **Circular dependency detection** | Fails fast at startup with `BeanCurrentlyInCreationException` | Allows circular dependencies (masks design smell) | Allows circular dependencies |
| **Spring recommendation** | **Preferred** (official recommendation since Spring 4.x) | Acceptable for optional/reconfigurable dependencies | **Discouraged** |

```java
// Constructor injection — preferred
@Service
public class OrderService {

    private final PaymentGateway paymentGateway;
    private final InventoryClient inventoryClient;

    // @Autowired is optional with a single constructor (Spring 4.3+)
    public OrderService(PaymentGateway paymentGateway, InventoryClient inventoryClient) {
        this.paymentGateway = paymentGateway;
        this.inventoryClient = inventoryClient;
    }
}

// Setter injection — acceptable for optional dependencies
@Service
public class NotificationService {

    private SmsSender smsSender;

    @Autowired(required = false)
    public void setSmsSender(SmsSender smsSender) {
        this.smsSender = smsSender;
    }
}

// Field injection — discouraged
@Service
public class BadService {
    @Autowired
    private SomeDependency dep; // Cannot be final, hard to test
}
```

**Why constructor injection is preferred:**

1. **Immutability** -- dependencies are `final`, making the class thread-safe by design.
2. **Completeness** -- the object is never in a half-initialized state.
3. **Testability** -- no Spring context needed in unit tests; just call `new OrderService(mockGateway, mockClient)`.
4. **Design pressure** -- a constructor with too many parameters signals the class has too many responsibilities (SRP violation), prompting refactoring.

---

### **Q34: What are the Spring bean scopes?**

A **bean scope** defines the lifecycle and visibility of a bean instance within the container.

| Scope | **Description** | **When Created** | **When Destroyed** | **Use Case** |
|---|---|---|---|---|
| **singleton** (default) | One instance per Spring IoC container | At container startup (eager) or first request (`@Lazy`) | Container shutdown | Stateless services, repositories, utilities |
| **prototype** | New instance every time the bean is requested | On each `getBean()` call or injection point | **Not** managed by Spring -- client code must clean up | Stateful, short-lived objects (commands, builders) |
| **request** | One instance per HTTP request | Start of HTTP request | End of HTTP request | Request-scoped DTOs, audit contexts |
| **session** | One instance per HTTP session | Session creation | Session invalidation/timeout | User preferences, shopping carts |
| **application** | One instance per `ServletContext` | `ServletContext` initialization | `ServletContext` destruction | App-wide shared config (rare -- similar to singleton) |

```java
@Component
@Scope("prototype")
public class ShoppingCart {
    private final List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        items.add(item);
    }
}
```

**Prototype-in-singleton pitfall:** Injecting a prototype-scoped bean into a singleton results in the *same* prototype instance being reused. Solutions:

1. **`ObjectFactory<T>` or `ObjectProvider<T>`** -- call `getObject()` each time.
2. **`@Lookup` method** -- Spring overrides the method to return a new prototype.
3. **Scoped proxy** -- `@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)`.

```java
@Service
public class CheckoutService {

    private final ObjectProvider<ShoppingCart> cartProvider;

    public CheckoutService(ObjectProvider<ShoppingCart> cartProvider) {
        this.cartProvider = cartProvider;
    }

    public void checkout() {
        ShoppingCart cart = cartProvider.getObject(); // Fresh prototype each time
        // ...
    }
}
```

---

### **Q35: Describe the Spring bean lifecycle in detail.**

The bean lifecycle in Spring follows a well-defined sequence from instantiation to destruction.

#### Full Lifecycle Flow

```
1. Instantiation (constructor call)
        ↓
2. Populate properties (dependency injection)
        ↓
3. BeanNameAware.setBeanName()
        ↓
4. BeanFactoryAware.setBeanFactory()
        ↓
5. ApplicationContextAware.setApplicationContext()
        ↓
6. BeanPostProcessor.postProcessBeforeInitialization()   ← e.g., @PostConstruct
        ↓
7. InitializingBean.afterPropertiesSet()
        ↓
8. Custom init-method (@Bean(initMethod = "init"))
        ↓
9. BeanPostProcessor.postProcessAfterInitialization()    ← AOP proxies created here
        ↓
      === Bean is ready for use ===
        ↓
10. @PreDestroy callback
        ↓
11. DisposableBean.destroy()
        ↓
12. Custom destroy-method (@Bean(destroyMethod = "cleanup"))
```

```java
@Component
public class CacheWarmer implements InitializingBean, DisposableBean {

    private final DataSource dataSource;

    public CacheWarmer(DataSource dataSource) { // Step 1 & 2
        this.dataSource = dataSource;
    }

    @PostConstruct                              // Step 6 (via CommonAnnotationBeanPostProcessor)
    public void postConstruct() {
        System.out.println("@PostConstruct — annotations processed");
    }

    @Override
    public void afterPropertiesSet() {          // Step 7
        System.out.println("InitializingBean.afterPropertiesSet()");
    }

    // Step 8 would be a custom init-method

    @PreDestroy                                 // Step 10
    public void preDestroy() {
        System.out.println("@PreDestroy — releasing resources");
    }

    @Override
    public void destroy() {                     // Step 11
        System.out.println("DisposableBean.destroy()");
    }
}
```

**Key points for senior interviews:**

- **`BeanPostProcessor`** is how Spring implements cross-cutting features. For example, `AutowiredAnnotationBeanPostProcessor` handles `@Autowired`, and `AnnotationAwareAspectJAutoProxyCreator` wraps beans in AOP proxies in step 9.
- **`@PostConstruct` / `@PreDestroy`** (from `jakarta.annotation`) are the recommended approach -- they decouple your code from Spring interfaces.
- **Prototype beans** do not receive destruction callbacks from the container -- Spring creates them and hands them off.
- **`SmartInitializingSingleton.afterSingletonsInstantiated()`** runs after *all* singletons are initialized -- useful for validation that depends on the full context being ready.

---

### **Q36: What is the difference between @Component, @Service, @Repository, and @Controller?**

These are **stereotype annotations** -- all are specializations of `@Component` and are detected by component scanning. They serve as semantic markers and, in some cases, enable additional behavior.

| Annotation | **Semantic Meaning** | **Additional Behavior** | **Typical Layer** |
|---|---|---|---|
| `@Component` | Generic Spring-managed bean | None -- base annotation | Any |
| `@Service` | Business logic / service layer | None beyond `@Component` (purely semantic) | Service / domain |
| `@Repository` | Data access / persistence layer | **Exception translation**: Catches platform-specific exceptions (e.g., `SQLException`, JPA `PersistenceException`) and translates them into Spring's `DataAccessException` hierarchy via `PersistenceExceptionTranslationPostProcessor` | DAO / repository |
| `@Controller` | Spring MVC controller (returns views) | Enables handler method detection by `DispatcherServlet`; works with `@RequestMapping` | Web / presentation |
| `@RestController` | `@Controller` + `@ResponseBody` | Every handler method implicitly has `@ResponseBody` -- return values serialized to JSON/XML directly | REST API |
| `@Configuration` | Configuration class holding `@Bean` definitions | Enables **CGLIB proxying** (full mode) to ensure `@Bean` inter-method calls return the same singleton | Configuration |

```java
@Repository
public class JpaOrderRepository implements OrderRepository {
    // SQLException → DataAccessException translation happens automatically
}

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

**Senior-level note:** While `@Service` adds no technical behavior beyond `@Component`, using the correct stereotype is important for:
- **AOP pointcut expressions** -- e.g., `@within(org.springframework.stereotype.Service)` targets only service beans.
- **Code readability** -- immediately signals the class's architectural role.
- **Static analysis tools** like ArchUnit that enforce layered architecture rules.

---

### **Q37: How do @Configuration and @Bean work? What is the difference between full mode and lite mode?**

`@Configuration` marks a class as a source of bean definitions. `@Bean` annotates methods that return objects to be managed by the container.

```java
@Configuration
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("https://api.example.com")
                .build();
    }

    @Bean
    public OrderClient orderClient(RestClient restClient) {
        // Spring injects the restClient bean automatically via method parameter
        return new OrderClient(restClient);
    }

    @Bean
    public AuditClient auditClient() {
        // Inter-method call — in FULL mode, this returns the SAME singleton
        return new AuditClient(restClient());
    }
}
```

#### Full Mode vs Lite Mode

| Aspect | **Full Mode** (`@Configuration`) | **Lite Mode** (`@Component` + `@Bean`) |
|---|---|---|
| **CGLIB proxy** | Yes -- the class is subclassed at runtime | No proxy |
| **Inter-method `@Bean` calls** | Return the **same singleton** instance (proxied) | Create a **new instance** every time (plain Java method call) |
| **Performance** | Slight overhead from CGLIB subclassing | Marginally faster startup |
| **When to use** | Default choice -- correct singleton semantics | When you explicitly want independent instances or are in a `@Component` class |

```java
// Lite mode — @Bean on a @Component class
@Component
public class LiteModeConfig {

    @Bean
    public Foo foo() {
        return new Foo();
    }

    @Bean
    public Bar bar() {
        // WARNING: This calls foo() as a plain Java method — creates a NEW Foo, not the singleton!
        return new Bar(foo());
    }
}
```

**Spring Boot 3.x note:** `@Configuration(proxyBeanMethods = false)` explicitly opts into lite mode while keeping the `@Configuration` annotation. Many Spring Boot auto-configuration classes use this for faster startup:

```java
@Configuration(proxyBeanMethods = false)
public class MyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyRepository repository) {
        return new MyService(repository);  // Use parameter injection, not inter-method calls
    }
}
```

---

### **Q38: What are circular dependencies and how do you fix them?**

A **circular dependency** occurs when bean A depends on bean B, and bean B (directly or transitively) depends on bean A.

```
BeanA → BeanB → BeanA  (direct cycle)
BeanA → BeanB → BeanC → BeanA  (transitive cycle)
```

**With constructor injection** (the recommended approach), Spring detects the cycle immediately at startup:

```
BeanCurrentlyInCreationException: Error creating bean with name 'beanA':
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

#### Why Circular Dependencies Are a Design Problem

Circular dependencies indicate **tight coupling** and often a violation of the **Single Responsibility Principle**. They signal that two classes are doing work that logically belongs together or that an intermediary abstraction is missing.

#### Solutions (Ordered by Preference)

**1. Redesign (best solution)**

Extract the shared logic into a third class that both depend on:

```java
// Before: A ↔ B (circular)
// After:  A → C ← B (no cycle)

@Service
public class SharedLogic {
    // Extracted common behavior
}

@Service
public class ServiceA {
    private final SharedLogic sharedLogic;
    public ServiceA(SharedLogic sharedLogic) { this.sharedLogic = sharedLogic; }
}

@Service
public class ServiceB {
    private final SharedLogic sharedLogic;
    public ServiceB(SharedLogic sharedLogic) { this.sharedLogic = sharedLogic; }
}
```

**2. Use events for decoupling**

```java
@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void placeOrder(Order order) {
        // ... process order
        eventPublisher.publishEvent(new OrderPlacedEvent(order.id()));
    }
}

@Service
public class NotificationService {

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        // No direct dependency on OrderService
    }
}
```

**3. `@Lazy` (workaround -- not a fix)**

```java
@Service
public class ServiceA {

    private final ServiceB serviceB;

    public ServiceA(@Lazy ServiceB serviceB) {
        // Injects a proxy; real ServiceB created on first method call
        this.serviceB = serviceB;
    }
}
```

`@Lazy` breaks the cycle by injecting a **proxy** instead of the real bean. The real bean is created lazily on first access. This is a workaround, not a solution -- the underlying design problem remains.

**Spring Boot 3.x note:** Since Spring Boot 2.6+, circular dependencies are **disallowed by default**. You must explicitly opt in with `spring.main.allow-circular-references=true`. This deliberate friction encourages proper redesign.

---

## Section 8: Spring Boot Auto-Configuration & Externalized Config

---

### **Q39: How does Spring Boot auto-configuration work?**

Auto-configuration is Spring Boot's mechanism for automatically configuring beans based on the classpath, existing beans, and properties. It follows a **convention-over-configuration** philosophy.

#### The Mechanism (Step by Step)

1. **`@SpringBootApplication`** includes `@EnableAutoConfiguration`.
2. `@EnableAutoConfiguration` imports `AutoConfigurationImportSelector`.
3. The selector reads candidate configuration classes from:
   - **Boot 2.x:** `META-INF/spring.factories` (key: `EnableAutoConfiguration`)
   - **Boot 3.x:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (one class per line, replaces `spring.factories` for auto-configuration)
4. Each candidate class is a `@Configuration` annotated with **`@Conditional*`** annotations.
5. Spring evaluates the conditions and only registers beans whose conditions are met.

#### Key @Conditional Annotations

| Annotation | **Condition** |
|---|---|
| `@ConditionalOnClass` | Class is present on the classpath |
| `@ConditionalOnMissingClass` | Class is absent from the classpath |
| `@ConditionalOnBean` | A specific bean already exists in the context |
| `@ConditionalOnMissingBean` | A specific bean does NOT exist (allows user to override) |
| `@ConditionalOnProperty` | A property has a specific value (or is present/absent) |
| `@ConditionalOnWebApplication` | The app is a web application (Servlet or Reactive) |
| `@ConditionalOnExpression` | A SpEL expression evaluates to `true` |

#### Example: How `DataSourceAutoConfiguration` Works

```java
@AutoConfiguration(before = SqlInitializationAutoConfiguration.class)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Conditional(EmbeddedDatabaseCondition.class)
    @ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
    @Import(EmbeddedDataSourceConfiguration.class)
    protected static class EmbeddedDatabaseConfiguration { }

    @Configuration(proxyBeanMethods = false)
    @Conditional(PooledDataSourceCondition.class)
    @ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
    @Import({ /* HikariCP, Tomcat, DBCP2 configs */ })
    protected static class PooledDataSourceConfiguration { }
}
```

**How to override:** Define your own `@Bean` of type `DataSource`, and `@ConditionalOnMissingBean` ensures the auto-configured one is skipped.

**Debugging tip:** Add `--debug` or set `debug=true` in `application.properties`. Spring Boot prints the **ConditionEvaluationReport** showing which auto-configurations matched, which did not, and why.

---

### **Q40: What is the property source precedence in Spring Boot?**

Spring Boot loads configuration from multiple sources. When the same property is defined in multiple sources, **higher-precedence sources override lower ones**.

#### Precedence Order (Highest to Lowest)

| Priority | **Source** | **Example** |
|---|---|---|
| 1 | Command-line arguments | `--server.port=9090` |
| 2 | `SPRING_APPLICATION_JSON` (inline JSON in env var or system property) | `SPRING_APPLICATION_JSON='{"server":{"port":9090}}'` |
| 3 | Servlet / `ServletContext` init parameters | `web.xml` params |
| 4 | JNDI attributes (`java:comp/env/`) | Application server JNDI |
| 5 | Java system properties (`-D`) | `-Dserver.port=9090` |
| 6 | OS environment variables | `SERVER_PORT=9090` (relaxed binding) |
| 7 | Profile-specific `application-{profile}.yml` **outside** jar | `config/application-prod.yml` |
| 8 | Profile-specific `application-{profile}.yml` **inside** jar | `classpath:application-prod.yml` |
| 9 | `application.yml` **outside** jar | `config/application.yml` |
| 10 | `application.yml` **inside** jar | `classpath:application.yml` |
| 11 | `@PropertySource` on `@Configuration` classes | `@PropertySource("classpath:custom.properties")` |
| 12 | Default properties (`SpringApplication.setDefaultProperties()`) | Fallback defaults |

**Relaxed binding for environment variables:** `SERVER_PORT` maps to `server.port`, `SPRING_DATASOURCE_URL` maps to `spring.datasource.url`. Spring Boot converts `_` to `.` and lowercases.

**Config tree (Kubernetes):** Spring Boot 3.x supports `spring.config.import=configtree:/etc/config/` which reads each file as a property value -- designed for Kubernetes ConfigMaps and Secrets mounted as volumes.

---

### **Q41: What is the difference between @Value and @ConfigurationProperties?**

Both bind external configuration to Java fields, but they differ significantly in type safety, validation, and structure.

| Aspect | **`@Value`** | **`@ConfigurationProperties`** |
|---|---|---|
| **Binding** | Individual property injection | Binds an entire prefix tree to a POJO |
| **Type safety** | Minimal -- SpEL evaluated at runtime | Full -- bound and validated at startup |
| **Relaxed binding** | Limited | Full (`myApp.db-url` = `MYAPP_DBURL` = `myapp.dbUrl`) |
| **Validation** | Not supported | `@Validated` + Bean Validation (`@NotBlank`, `@Min`, etc.) |
| **Meta-data** | No IDE auto-complete | `spring-boot-configuration-processor` generates `META-INF/spring-configuration-metadata.json` for IDE auto-complete |
| **Immutability** | Mutable only | Supports constructor binding (`@ConstructorBinding`) for immutable records |
| **SpEL** | Supported (`#{...}`) | Not supported |
| **Best for** | Simple, one-off values | Structured configuration groups |

```java
// @Value — quick and simple
@Component
public class SimpleConfig {

    @Value("${app.max-retries:3}")
    private int maxRetries;

    @Value("#{${app.feature-flags}}") // SpEL — parses a map
    private Map<String, Boolean> featureFlags;
}

// @ConfigurationProperties — structured and validated (preferred)
@ConfigurationProperties(prefix = "app.payment")
@Validated
public record PaymentProperties(
        @NotBlank String gatewayUrl,
        @Min(1) int maxRetries,
        @DurationUnit(ChronoUnit.SECONDS) Duration timeout,
        Retry retry
) {
    public record Retry(int maxAttempts, Duration backoff) { }
}
```

```yaml
# application.yml
app:
  payment:
    gateway-url: https://pay.example.com
    max-retries: 3
    timeout: 30s
    retry:
      max-attempts: 5
      backoff: 2s
```

```java
// Enabling the properties class
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfig { }
```

**Senior-level recommendation:** Prefer `@ConfigurationProperties` for anything beyond a single trivial value. The type safety, validation, and IDE support significantly reduce runtime configuration errors.

---

### **Q42: How do Spring profiles work?**

**Profiles** allow you to segregate parts of your application configuration and bean definitions so they are only active in certain environments.

#### Activating Profiles

```bash
# Environment variable
SPRING_PROFILES_ACTIVE=prod,metrics

# Command line
java -jar app.jar --spring.profiles.active=prod,metrics

# application.yml
spring:
  profiles:
    active: dev
```

#### Profile-Specific Property Files

```
application.yml              ← always loaded
application-dev.yml          ← loaded when "dev" profile is active
application-prod.yml         ← loaded when "prod" profile is active
application-test.yml         ← loaded when "test" profile is active
```

Profile-specific files **override** values in the base `application.yml`.

#### Profile Groups (Spring Boot 2.4+)

```yaml
# application.yml
spring:
  profiles:
    group:
      production: "prod,metrics,ssl"
      staging: "stage,metrics"
```

Now activating `production` automatically activates `prod`, `metrics`, and `ssl`.

#### Conditional Beans with @Profile

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Profile("dev")
    public DataSource embeddedDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();
    }

    @Bean
    @Profile("prod")
    public DataSource productionDataSource(DataSourceProperties props) {
        return DataSourceBuilder.create()
                .url(props.getUrl())
                .username(props.getUsername())
                .password(props.getPassword())
                .build();
    }
}
```

#### Profile Expressions (Spring 5.1+)

```java
@Profile("prod & !eu-west")     // AND + NOT
@Profile("staging | qa")        // OR
```

**Spring Boot 3.x Config Import:**

```yaml
spring:
  config:
    import:
      - classpath:db-config.yml
      - optional:file:/etc/app/secrets.yml  # "optional:" prevents failure if missing
```

---

### **Q43: When and how would you create a custom auto-configuration?**

Custom auto-configuration is appropriate when you are building a **shared library** consumed by multiple Spring Boot applications -- for example, a company-wide logging framework, a custom metrics collector, or an internal SDK.

#### When to Create Custom Auto-Configuration

- **Shared starter library** consumed by multiple microservices
- **Open-source library** that wants to offer plug-and-play Spring Boot support
- **NOT** for application-level configuration in a single service -- use `@Configuration` classes directly

#### Step-by-Step Implementation

**1. Create the configuration class:**

```java
@AutoConfiguration
@ConditionalOnClass(AuditClient.class)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditClient auditClient(AuditProperties properties) {
        return new AuditClient(properties.endpoint(), properties.apiKey());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "audit", name = "async-enabled", havingValue = "true")
    public AsyncAuditDecorator asyncAuditDecorator(AuditClient auditClient) {
        return new AsyncAuditDecorator(auditClient);
    }
}
```

**2. Create the properties class:**

```java
@ConfigurationProperties(prefix = "audit")
public record AuditProperties(
        @NotBlank String endpoint,
        @NotBlank String apiKey,
        boolean asyncEnabled
) { }
```

**3. Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:**

```
com.example.audit.AuditAutoConfiguration
```

**4. Create a Spring Boot starter (optional but conventional):**

```
audit-spring-boot-starter/          ← starter module (just dependencies, no code)
  └── pom.xml                       ← depends on audit-spring-boot-autoconfigure

audit-spring-boot-autoconfigure/    ← auto-configuration module
  └── src/main/java/...
  └── src/main/resources/META-INF/spring/...AutoConfiguration.imports
```

**Ordering auto-configurations:**

```java
@AutoConfiguration(
    after = DataSourceAutoConfiguration.class,
    before = FlywayAutoConfiguration.class
)
public class AuditAutoConfiguration { }
```

**Testing auto-configurations with `ApplicationContextRunner`:**

```java
class AuditAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    void createsAuditClientWhenPropertiesSet() {
        runner.withPropertyValues(
                        "audit.endpoint=https://audit.example.com",
                        "audit.api-key=secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditClient.class);
                    assertThat(context).doesNotHaveBean(AsyncAuditDecorator.class);
                });
    }

    @Test
    void userCanOverrideAuditClient() {
        runner.withPropertyValues("audit.endpoint=x", "audit.api-key=y")
                .withBean(AuditClient.class, () -> new AuditClient("custom", "key"))
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditClient.class);
                });
    }
}
```

---

## Section 9: Spring MVC & REST APIs

---

### **Q44: What is the difference between @RestController and @Controller?**

| Annotation | **Definition** | **Response Handling** | **Typical Use Case** |
|---|---|---|---|
| `@Controller` | `@Component` + handler-method detection | Returns a **view name** (resolved by `ViewResolver` to a template like Thymeleaf) | Server-rendered web pages |
| `@RestController` | `@Controller` + `@ResponseBody` on every method | Return value is **serialized directly** to the HTTP response body (JSON/XML via `HttpMessageConverter`) | REST APIs |

```java
// @Controller — returns a view name
@Controller
public class PageController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", statsService.getCurrent());
        return "dashboard"; // Resolved to templates/dashboard.html
    }

    // If you need JSON from a @Controller, annotate the method:
    @GetMapping("/api/stats")
    @ResponseBody
    public Stats stats() {
        return statsService.getCurrent(); // Serialized to JSON
    }
}

// @RestController — every method returns serialized body
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.findById(id); // Automatically serialized to JSON
    }
}
```

**Under the hood:** When `@ResponseBody` is present, Spring selects an `HttpMessageConverter` based on the `Accept` header. The default for JSON is `MappingJackson2HttpMessageConverter` (Jackson).

---

### **Q45: Explain Spring MVC request mapping annotations with examples.**

Spring provides composed annotations that combine `@RequestMapping` with an HTTP method for cleaner, more readable code.

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // @PathVariable — extracts value from URI path
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("orderId") UUID orderId) {
        return orderService.findById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // @RequestParam — extracts query parameters
    @GetMapping
    public Page<OrderDto> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {
        return orderService.findAll(status, PageRequest.of(page, size));
    }

    // @RequestBody — deserializes JSON request body to Java object
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderDto created = orderService.create(request);
        URI location = URI.create("/api/v1/orders/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    // @RequestHeader — extracts HTTP headers
    @PutMapping("/{orderId}")
    public OrderDto updateOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody UpdateOrderRequest request) {
        return orderService.update(orderId, request, idempotencyKey);
    }

    // @DeleteMapping with no response body
    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable UUID orderId) {
        orderService.cancel(orderId);
    }
}
```

| Annotation | **HTTP Method** | **Typical Use** |
|---|---|---|
| `@GetMapping` | GET | Retrieve resource(s) |
| `@PostMapping` | POST | Create a resource |
| `@PutMapping` | PUT | Full update / replace |
| `@PatchMapping` | PATCH | Partial update |
| `@DeleteMapping` | DELETE | Remove a resource |

**Method argument resolution:** Spring MVC uses `HandlerMethodArgumentResolver` implementations to populate each parameter. Custom resolvers can be registered for domain-specific types.

---

### **Q46: How do you handle exceptions in Spring Boot 3.x REST APIs?**

Spring Boot 3.x embraces **RFC 7807 Problem Details** (`application/problem+json`) as the standard error response format.

#### Centralized Exception Handling with @ControllerAdvice

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Business exception → 404
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleNotFound(OrderNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setType(URI.create("https://api.example.com/errors/order-not-found"));
        problem.setProperty("orderId", ex.getOrderId()); // Custom extension field
        return problem;
    }

    // Validation errors → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", Objects.requireNonNull(fe.getDefaultMessage()),
                        "rejected", String.valueOf(fe.getRejectedValue())))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    // Catch-all → 500
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // Log the full stack trace internally
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
```

**Resulting JSON response:**

```json
{
  "type": "https://api.example.com/errors/order-not-found",
  "title": "Order Not Found",
  "status": 404,
  "detail": "Order with ID 550e8400-e29b-41d4-a716-446655440000 not found",
  "instance": "/api/v1/orders/550e8400-e29b-41d4-a716-446655440000",
  "orderId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Enabling RFC 7807 globally (Spring Boot 3.x):**

```yaml
# application.yml
spring:
  mvc:
    problemdetails:
      enabled: true  # Default is true in Boot 3.x
```

With this enabled, Spring Boot automatically translates standard Spring exceptions (e.g., `HttpRequestMethodNotSupportedException`, `TypeMismatchException`) into `ProblemDetail` responses without any custom handler.

**`ErrorResponse` interface:** In Spring 6, exceptions can implement `ErrorResponse` to carry their own `ProblemDetail`:

```java
public class OrderNotFoundException extends RuntimeException implements ErrorResponse {

    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super("Order with ID " + orderId + " not found");
        this.orderId = orderId;
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public ProblemDetail getBody() {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());
        pd.setTitle("Order Not Found");
        pd.setProperty("orderId", orderId);
        return pd;
    }
}
```

---

### **Q47: How does validation work in Spring Boot? How do you create custom validators?**

Spring Boot integrates **Jakarta Bean Validation** (Hibernate Validator as the default implementation) with Spring MVC and Spring Data.

#### Basic Usage

```java
public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotEmpty List<@Valid OrderItem> items,
        @FutureOrPresent LocalDate deliveryDate
) { }

public record OrderItem(
        @NotNull UUID productId,
        @Min(1) @Max(10_000) int quantity
) { }
```

```java
@PostMapping
public ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
    // If validation fails, MethodArgumentNotValidException is thrown automatically
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
}
```

#### @Valid vs @Validated

| Aspect | **`@Valid`** (Jakarta) | **`@Validated`** (Spring) |
|---|---|---|
| **Source** | `jakarta.validation.Valid` | `org.springframework.validation.annotation.Validated` |
| **Validation groups** | Not supported | Supported |
| **Method-level validation** | Not supported | Enables `MethodValidationPostProcessor` |
| **Cascading** | Yes (validates nested objects) | Yes |

```java
// Validation groups
public interface OnCreate { }
public interface OnUpdate { }

public record ProductRequest(
        @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) UUID id,
        @NotBlank(groups = { OnCreate.class, OnUpdate.class }) String name,
        @Positive BigDecimal price
) { }

@PostMapping
public ResponseEntity<Product> create(
        @RequestBody @Validated(OnCreate.class) ProductRequest request) { ... }

@PutMapping("/{id}")
public Product update(@PathVariable UUID id,
        @RequestBody @Validated(OnUpdate.class) ProductRequest request) { ... }
```

#### Custom Validator

```java
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = NoProfanityValidator.class)
public @interface NoProfanity {
    String message() default "Text contains inappropriate content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private ProfanityFilter filter;

    @Override
    public void initialize(NoProfanity annotation) {
        this.filter = new ProfanityFilter(); // Or inject via constructor in Spring 6+
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // Let @NotNull handle null checks
        return !filter.containsProfanity(value);
    }
}
```

#### Cross-Field Validation (Class-Level)

```java
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "Start date must be before end date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@ValidDateRange
public record DateRangeRequest(
        LocalDate startDate,
        LocalDate endDate
) { }
```

---

### **Q48: How do you handle content negotiation and proper HTTP status codes?**

#### ResponseEntity for Full Control

```java
@GetMapping("/{id}")
public ResponseEntity<ProductDto> getProduct(@PathVariable UUID id) {
    return productService.findById(id)
            .map(p -> ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                    .eTag(p.version().toString())
                    .body(p))
            .orElseGet(() -> ResponseEntity.notFound().build());
}

@PostMapping
public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProductRequest request) {
    ProductDto created = productService.create(request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
}
```

#### HTTP Methods and Status Codes Reference

| HTTP Method | **Success Status** | **Semantics** | **Idempotent** | **Request Body** |
|---|---|---|---|---|
| `GET` | `200 OK` | Retrieve a resource | Yes | No |
| `POST` | `201 Created` | Create a resource | No | Yes |
| `PUT` | `200 OK` or `204 No Content` | Full replace | Yes | Yes |
| `PATCH` | `200 OK` | Partial update | No* | Yes |
| `DELETE` | `204 No Content` | Remove a resource | Yes | No |
| `HEAD` | `200 OK` (no body) | Same as GET without body | Yes | No |
| `OPTIONS` | `200 OK` | Describe communication options | Yes | No |

*PATCH can be made idempotent depending on implementation (e.g., JSON Merge Patch).

#### Common Error Status Codes

| Code | **Meaning** | **When to Use** |
|---|---|---|
| `400` | Bad Request | Validation failure, malformed JSON |
| `401` | Unauthorized | Missing or invalid authentication |
| `403` | Forbidden | Authenticated but insufficient permissions |
| `404` | Not Found | Resource does not exist |
| `409` | Conflict | Duplicate resource, optimistic lock failure |
| `422` | Unprocessable Entity | Semantic validation failure (valid JSON but invalid business rules) |
| `429` | Too Many Requests | Rate limiting |
| `500` | Internal Server Error | Unexpected server-side failure |

#### Content Negotiation

Spring MVC resolves the response format using (in priority order):
1. **URL suffix** (disabled by default in Boot 3.x for security)
2. **`Accept` header** (`application/json`, `application/xml`)
3. **`format` query parameter** (if configured)

```yaml
# application.yml — enable XML support
spring:
  mvc:
    contentnegotiation:
      favor-parameter: true
      parameter-name: format
```

Add `jackson-dataformat-xml` to the classpath and both JSON and XML are automatically supported.

---

### **Q49: What are the differences between Filters, Interceptors, and AOP in the Spring request pipeline?**

| Aspect | **Servlet Filter** | **Spring HandlerInterceptor** | **Spring AOP** |
|---|---|---|---|
| **Specification** | Jakarta Servlet API | Spring MVC | Spring AOP / AspectJ |
| **Scope** | All requests (including static resources) | Only requests handled by `DispatcherServlet` | Any Spring bean method |
| **Access to** | `HttpServletRequest` / `HttpServletResponse` | Request + Handler (`HandlerMethod`) + `ModelAndView` | Method arguments, return value, exception |
| **Execution order** | Before/after `DispatcherServlet` | Before/after handler execution | Around bean method invocation |
| **Use cases** | CORS, authentication, request logging, compression, rate limiting | Logging, authorization, locale/theme resolution, handler timing | Transaction management, caching, retry, auditing |
| **Registration** | `FilterRegistrationBean` or `@WebFilter` | `WebMvcConfigurer.addInterceptors()` | `@Aspect` + `@Around`/`@Before`/`@After` |
| **Can short-circuit?** | Yes (do not call `chain.doFilter()`) | Yes (return `false` from `preHandle()`) | Yes (do not call `joinPoint.proceed()`) |

#### Request Processing Pipeline

```
HTTP Request
    │
    ▼
┌─────────────────┐
│  Servlet Filter  │  ← Security filter, CORS filter, logging filter
│  chain           │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ DispatcherServlet│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ HandlerInterceptor│ preHandle()
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Controller      │  ← AOP proxies wrap the controller/service method
│  (Handler)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ HandlerInterceptor│ postHandle() / afterCompletion()
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Servlet Filter  │  (response phase)
│  chain           │
└─────────────────┘
```

```java
// Servlet Filter — timing and request ID
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("Completed {} {} in {}ms [status={}]",
                    request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            MDC.clear();
        }
    }
}

// HandlerInterceptor — access to handler metadata
@Component
public class AuditInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) {
        if (handler instanceof HandlerMethod hm) {
            Audited audited = hm.getMethodAnnotation(Audited.class);
            if (audited != null) {
                log.info("Audit: {} invoked by {}",
                        hm.getMethod().getName(), request.getUserPrincipal());
            }
        }
        return true; // Continue processing
    }
}
```

---

## Section 10: Spring Data JPA & Persistence

---

### **Q50: Explain the Spring Data repository abstraction and the differences between CrudRepository, JpaRepository, and custom queries.**

Spring Data JPA eliminates boilerplate DAO code by providing repository interfaces with built-in CRUD and query derivation.

#### Repository Hierarchy

| Interface | **Extends** | **Key Methods Added** | **When to Use** |
|---|---|---|---|
| `Repository<T, ID>` | -- | Marker interface (no methods) | When you want to cherry-pick methods with `@RepositoryDefinition` |
| `CrudRepository<T, ID>` | `Repository` | `save()`, `findById()`, `findAll()`, `deleteById()`, `count()`, `existsById()` | Basic CRUD without pagination |
| `ListCrudRepository<T, ID>` | `CrudRepository` | Returns `List` instead of `Iterable` | When you need `List` return types |
| `PagingAndSortingRepository<T, ID>` | `Repository` | `findAll(Pageable)`, `findAll(Sort)` | When pagination is needed |
| `JpaRepository<T, ID>` | `ListCrudRepository` + `ListPagingAndSortingRepository` | `flush()`, `saveAndFlush()`, `deleteAllInBatch()`, `getReferenceById()`, `findAll(Example)` | Default choice for JPA -- provides the most functionality |

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Derived query — Spring parses the method name
    List<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(UUID customerId, OrderStatus status);

    // Explicit JPQL
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    // Native SQL
    @Query(value = "SELECT * FROM orders WHERE total_amount > :amount", nativeQuery = true)
    List<Order> findExpensiveOrders(@Param("amount") BigDecimal amount);

    // Modifying query
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") OrderStatus status);

    // Projection
    @Query("SELECT o.id as id, o.status as status, o.totalAmount as totalAmount FROM Order o")
    List<OrderSummary> findAllSummaries();
}

// Interface-based projection
public interface OrderSummary {
    UUID getId();
    OrderStatus getStatus();
    BigDecimal getTotalAmount();
}
```

**`getReferenceById()` vs `findById()`:** `getReferenceById()` returns a lazy proxy (Hibernate `getReference()`) without hitting the database -- useful when you only need the ID for setting foreign-key relationships. `findById()` executes a SELECT immediately.

---

### **Q51: How do you map entities with JPA annotations? What are the @GeneratedValue strategies?**

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_customer", columnList = "customer_id"),
    @Index(name = "idx_order_status", columnList = "status")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version; // Optimistic locking

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors, getters, setters omitted for brevity
}
```

#### @GeneratedValue Strategies

| Strategy | **Mechanism** | **Database Support** | **Batch Insert Performance** | **Portable** |
|---|---|---|---|---|
| `IDENTITY` | Auto-increment column (`SERIAL`, `AUTO_INCREMENT`) | PostgreSQL, MySQL, SQL Server | **Poor** -- Hibernate must do `INSERT` immediately to get the generated ID; disables JDBC batch inserts | Yes |
| `SEQUENCE` | Database sequence object | PostgreSQL, Oracle, H2 | **Excellent** -- Hibernate pre-allocates IDs with `allocationSize`, enabling batch inserts | Sequence-supporting DBs |
| `TABLE` | Simulates sequences with a table | All databases | **Poor** -- requires row-level locking on the table | Yes but slow |
| `UUID` (Hibernate 6+) | UUID generation | All databases | **Excellent** -- generated in memory | Yes |
| `AUTO` (default) | Hibernate chooses based on dialect | Varies | Depends on chosen strategy | Yes |

**Recommended for Spring Boot 3.x / Hibernate 6+:**

```java
// For PostgreSQL — use SEQUENCE for batch insert performance
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
@SequenceGenerator(name = "order_seq", sequenceName = "order_id_seq", allocationSize = 50)
private Long id;

// For UUID primary keys — Hibernate 6+ UuidGenerator
@Id
@GeneratedValue
@UuidGenerator(style = UuidGenerator.Style.TIME)
private UUID id;
```

---

### **Q52: How do you map entity relationships? Explain fetch types and cascading.**

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many orders belong to one customer
    @ManyToOne(fetch = FetchType.LAZY)       // LAZY is not the default for @ManyToOne!
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // One order has many line items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Convenience methods to maintain bidirectional consistency
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int quantity;
    private BigDecimal unitPrice;
}

// Many-to-Many with explicit join entity (preferred over @ManyToMany)
@Entity
@Table(name = "student_courses")
public class StudentCourse {

    @EmbeddedId
    private StudentCourseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDate enrolledAt;
    private String grade;
}
```

#### Default Fetch Types

| Relationship | **Default Fetch** | **Recommended** |
|---|---|---|
| `@ManyToOne` | **EAGER** | `FetchType.LAZY` (override the default!) |
| `@OneToOne` | **EAGER** | `FetchType.LAZY` (but requires bytecode enhancement for optional owning side) |
| `@OneToMany` | LAZY | LAZY (already correct) |
| `@ManyToMany` | LAZY | LAZY (already correct) |

#### Cascade Types

| Type | **Behavior** |
|---|---|
| `PERSIST` | Save child when parent is saved |
| `MERGE` | Update child when parent is merged |
| `REMOVE` | Delete child when parent is deleted |
| `REFRESH` | Refresh child when parent is refreshed |
| `DETACH` | Detach child when parent is detached |
| `ALL` | All of the above |

**`orphanRemoval = true`** goes further than `CascadeType.REMOVE` -- if a child entity is *removed from the collection* (not just when the parent is deleted), it is deleted from the database.

**Senior-level warning about `@ManyToMany`:** Avoid `@ManyToMany` with a `@JoinTable` in production code. Use an explicit join entity instead, because:
1. You almost always need additional columns on the join table (timestamps, status).
2. The implicit join table gives you less control over DDL and indexing.
3. Cascade and orphan removal behaviors become unpredictable.

---

### **Q53: Compare JPQL, native queries, Criteria API, and Specifications.**

| Aspect | **JPQL** | **Native SQL** | **Criteria API** | **Specifications** |
|---|---|---|---|---|
| **Syntax** | Entity/property names | Database table/column names | Programmatic Java API | Reusable `Predicate` building blocks |
| **Type safety** | No (string-based) | No (string-based) | Yes (with metamodel) | Yes (with metamodel) |
| **Database portability** | Yes (dialect-independent) | No (SQL dialect-specific) | Yes | Yes |
| **Dynamic queries** | Hard (string concatenation) | Hard | **Designed for it** | **Designed for it** |
| **Readability** | High for simple queries | High for complex SQL | Low (verbose builder API) | Medium (composable) |
| **Use case** | Static queries known at compile time | DB-specific features (window functions, CTEs, JSON operators) | Dynamic filters with many optional criteria | Reusable, composable query predicates |

```java
// JPQL — @Query annotation
@Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt > :since")
List<Order> findRecentByStatus(@Param("status") OrderStatus status,
                                @Param("since") Instant since);

// Native SQL — when you need DB-specific features
@Query(value = """
    SELECT o.*, RANK() OVER (PARTITION BY o.customer_id ORDER BY o.total_amount DESC) as rnk
    FROM orders o
    WHERE o.status = :status
    """, nativeQuery = true)
List<Order> findWithRanking(@Param("status") String status);
```

```java
// Specification — composable predicates
public class OrderSpecifications {

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAfter(Instant since) {
        return (root, query, cb) -> cb.greaterThan(root.get("createdAt"), since);
    }

    public static Specification<Order> totalAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("totalAmount"), min, max);
    }
}

// Usage — compose specifications dynamically
public interface OrderRepository extends JpaRepository<Order, UUID>,
                                          JpaSpecificationExecutor<Order> { }

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public Page<Order> search(OrderSearchCriteria criteria, Pageable pageable) {
        Specification<Order> spec = Specification.where(null);

        if (criteria.status() != null) {
            spec = spec.and(OrderSpecifications.hasStatus(criteria.status()));
        }
        if (criteria.since() != null) {
            spec = spec.and(OrderSpecifications.createdAfter(criteria.since()));
        }
        if (criteria.minAmount() != null && criteria.maxAmount() != null) {
            spec = spec.and(OrderSpecifications.totalAmountBetween(
                    criteria.minAmount(), criteria.maxAmount()));
        }

        return orderRepository.findAll(spec, pageable);
    }
}
```

---

### **Q54: Explain lazy loading vs eager loading. What is LazyInitializationException and how do you solve it?**

**Lazy loading** defers database queries for associated entities until the association is actually accessed. **Eager loading** fetches associated entities immediately along with the parent entity.

**`LazyInitializationException`** occurs when you access a lazy-loaded association **outside** an active persistence context (i.e., after the `Session`/`EntityManager` is closed -- typically after the `@Transactional` method returns).

```java
// This WILL throw LazyInitializationException
@GetMapping("/orders/{id}")
public OrderDto getOrder(@PathVariable UUID id) {
    Order order = orderRepository.findById(id).orElseThrow();
    // Session is closed at this point (no open-in-view in production)
    order.getItems().size(); // BOOM — LazyInitializationException
}
```

#### Solutions (Ordered by Preference)

**1. JOIN FETCH in JPQL (most common and explicit)**

```java
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") UUID id);
```

**2. @EntityGraph (declarative)**

```java
@EntityGraph(attributePaths = {"items", "items.product"})
Optional<Order> findWithItemsById(UUID id);

// Named entity graph
@Entity
@NamedEntityGraph(name = "Order.withItems",
    attributeNodes = @NamedAttributeNode("items"))
public class Order { ... }

@EntityGraph("Order.withItems")
Optional<Order> findDetailedById(UUID id);
```

**3. DTO projection (best for read-only use cases)**

```java
@Query("""
    SELECT new com.example.dto.OrderSummaryDto(o.id, o.status, o.totalAmount, i.productName, i.quantity)
    FROM Order o JOIN o.items i WHERE o.id = :id
    """)
List<OrderSummaryDto> findOrderSummary(@Param("id") UUID id);
```

**4. Hibernate.initialize() within @Transactional**

```java
@Transactional(readOnly = true)
public Order getOrderWithItems(UUID id) {
    Order order = orderRepository.findById(id).orElseThrow();
    Hibernate.initialize(order.getItems()); // Forces loading within the session
    return order;
}
```

**Anti-pattern: Open Session in View (OSIV)**

Spring Boot enables `spring.jpa.open-in-view=true` by default. This keeps the `EntityManager` open for the entire HTTP request, allowing lazy loading in the view layer. While convenient, this is an **anti-pattern** in production because:
- It can trigger unexpected N+1 queries in the presentation layer.
- It holds database connections longer than necessary.
- It blurs the boundary between service and presentation logic.

**Recommendation:** Disable OSIV and fetch exactly what you need in the service layer:

```yaml
spring:
  jpa:
    open-in-view: false
```

#### The N+1 Problem

```java
// N+1 problem: 1 query for orders + N queries for items
List<Order> orders = orderRepository.findAll(); // 1 query
for (Order order : orders) {
    order.getItems().size(); // N queries (one per order)
}

// Solution: JOIN FETCH or @EntityGraph
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();
```

---

### **Q55: How does pagination and sorting work in Spring Data JPA? What is the difference between Page and Slice?**

Spring Data provides `Pageable`, `Page`, `Slice`, and `Sort` abstractions that translate directly to SQL `LIMIT`/`OFFSET` and `ORDER BY`.

```java
// Controller
@GetMapping
public Page<OrderDto> listOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
    return orderRepository.findByStatus(OrderStatus.ACTIVE, pageable)
            .map(orderMapper::toDto);
}

// Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Slice<Order> findByCustomerId(UUID customerId, Pageable pageable);

    // Sorting with derived queries
    List<Order> findByStatusOrderByTotalAmountDesc(OrderStatus status);
}
```

#### Page vs Slice

| Aspect | **`Page<T>`** | **`Slice<T>`** |
|---|---|---|
| **Total element count** | Yes (`getTotalElements()`) | No |
| **Total page count** | Yes (`getTotalPages()`) | No |
| **SQL queries** | **Two** queries: data + `SELECT COUNT(*)` | **One** query: fetches `size + 1` rows to determine `hasNext()` |
| **Performance** | Slower for large tables (COUNT can be expensive) | Faster -- no count query |
| **Use case** | Traditional paginated UIs with page numbers | Infinite scroll / "Load more" UIs |
| **Knows if next page exists** | Yes | Yes (by fetching one extra row) |

```java
// Page JSON response (Spring Boot default via PagedModel)
{
  "content": [ ... ],
  "pageable": { "pageNumber": 0, "pageSize": 20, ... },
  "totalElements": 458,
  "totalPages": 23,
  "last": false,
  "first": true,
  "number": 0,
  "size": 20,
  "numberOfElements": 20
}
```

**Custom Sort:**

```java
Sort sort = Sort.by(
    Sort.Order.desc("status"),
    Sort.Order.asc("createdAt")
);
Pageable pageable = PageRequest.of(0, 20, sort);

// Type-safe sort using JPA metamodel (generated)
Sort sort = Sort.by(Sort.Order.desc(Order_.STATUS), Sort.Order.asc(Order_.CREATED_AT));
```

**Keyset pagination (cursor-based) -- for large datasets:**

Spring Data 3.1+ introduced `ScrollPosition` and `Window` for keyset-based pagination, which avoids the performance degradation of `OFFSET` on deep pages:

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Window<Order> findByStatus(OrderStatus status, ScrollPosition position, Limit limit, Sort sort);
}

// Usage
Window<Order> firstPage = orderRepository.findByStatus(
        OrderStatus.ACTIVE,
        ScrollPosition.keyset(),  // Start from the beginning
        Limit.of(20),
        Sort.by("createdAt").descending());

// Next page — uses the last row's sort key instead of OFFSET
if (firstPage.hasNext()) {
    Window<Order> nextPage = orderRepository.findByStatus(
            OrderStatus.ACTIVE,
            firstPage.positionAt(firstPage.size() - 1), // Keyset of last element
            Limit.of(20),
            Sort.by("createdAt").descending());
}
```

---

## Section 11: Transaction Management

---

### **Q56: How does @Transactional work? Why does self-invocation not trigger a transaction?**

`@Transactional` is implemented via **proxy-based AOP**. Spring creates a proxy (JDK dynamic proxy or CGLIB subclass) that wraps the target bean. The proxy intercepts method calls and manages the transaction lifecycle.

#### How the Proxy Works

```
Caller  →  Proxy (TransactionInterceptor)  →  Target Bean
           1. Get transaction
           2. Begin or join transaction
                     → target.method()
           3a. Commit (if no exception)
           3b. Rollback (if RuntimeException)
```

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Transactional
    public void placeOrder(OrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);
        paymentService.charge(order); // If this throws, entire TX rolls back
    }
}
```

#### Why Self-Invocation Bypasses @Transactional

When a method within the same class calls another `@Transactional` method, it calls `this.method()` directly -- bypassing the proxy:

```java
@Service
public class OrderService {

    @Transactional
    public void processOrders(List<OrderRequest> requests) {
        for (OrderRequest req : requests) {
            placeOrder(req); // ← Self-invocation: calls this.placeOrder(), NOT the proxy
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void placeOrder(OrderRequest request) {
        // REQUIRES_NEW is IGNORED because the proxy was bypassed
    }
}
```

#### Solutions to Self-Invocation

**1. Extract to a separate bean (preferred)**

```java
@Service
public class OrderBatchService {
    private final OrderService orderService; // Injected — goes through proxy

    @Transactional
    public void processOrders(List<OrderRequest> requests) {
        for (OrderRequest req : requests) {
            orderService.placeOrder(req); // ← Goes through the proxy
        }
    }
}
```

**2. Self-inject via `ObjectProvider` (acceptable)**

```java
@Service
public class OrderService {

    private final ObjectProvider<OrderService> self;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void placeOrder(OrderRequest request) { ... }

    @Transactional
    public void processOrders(List<OrderRequest> requests) {
        OrderService proxy = self.getObject();
        for (OrderRequest req : requests) {
            proxy.placeOrder(req); // Goes through the proxy
        }
    }
}
```

**3. AspectJ weaving (compile-time or load-time)**

With AspectJ mode (`@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)`), the transaction advice is woven directly into the bytecode, so self-invocation works. This requires the AspectJ weaver and is less commonly used.

---

### **Q57: What are the transaction propagation levels in Spring?**

**Propagation** defines how a transactional method behaves when called within the context of an existing transaction.

| Propagation | **Existing TX** | **No Existing TX** | **Use Case** |
|---|---|---|---|
| `REQUIRED` (default) | Join the existing TX | Create a new TX | Standard business operations |
| `REQUIRES_NEW` | **Suspend** existing TX, create new TX | Create a new TX | Audit logging, independent operations that must commit regardless |
| `NESTED` | Create a **savepoint** within existing TX | Create a new TX | Partial rollback within a larger TX (requires JDBC savepoint support) |
| `SUPPORTS` | Join the existing TX | Execute **non-transactionally** | Read operations that can work with or without a TX |
| `NOT_SUPPORTED` | **Suspend** existing TX, execute non-transactionally | Execute non-transactionally | Operations that must not run in a TX (e.g., long-running reads) |
| `MANDATORY` | Join the existing TX | Throw `IllegalTransactionStateException` | Enforce that caller must provide a TX |
| `NEVER` | Throw `IllegalTransactionStateException` | Execute non-transactionally | Enforce that no TX is active |

```java
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    @Transactional // Default: REQUIRED
    public void transfer(UUID fromId, UUID toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.debit(amount);
        to.credit(amount);

        accountRepository.save(from);
        accountRepository.save(to);

        // Audit log is committed even if outer TX rolls back later
        auditService.logTransfer(fromId, toId, amount);
    }
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransfer(UUID fromId, UUID toId, BigDecimal amount) {
        // Runs in its own transaction — committed independently
        auditRepository.save(new AuditEntry("TRANSFER", fromId, toId, amount));
    }
}
```

**`NESTED` vs `REQUIRES_NEW`:**
- `REQUIRES_NEW` creates a completely **independent** transaction -- it commits even if the outer transaction rolls back.
- `NESTED` creates a **savepoint** -- if the nested portion fails, only changes after the savepoint are rolled back, and the outer transaction can continue. If the outer transaction rolls back, the nested changes roll back too.

---

### **Q58: How do isolation levels work in Spring's @Transactional?**

Spring's `@Transactional(isolation = ...)` maps directly to database isolation levels. The Spring `Isolation` enum values correspond to the SQL standard levels (see [Section 15](#15-transactions--isolation-levels) for the database-level explanation).

| Spring Isolation | **SQL Level** | **Dirty Read** | **Non-Repeatable Read** | **Phantom Read** | **Performance** |
|---|---|---|---|---|---|
| `DEFAULT` | Use the DB default | Depends on DB | Depends on DB | Depends on DB | Depends on DB |
| `READ_UNCOMMITTED` | Read Uncommitted | Possible | Possible | Possible | Fastest |
| `READ_COMMITTED` | Read Committed | Prevented | Possible | Possible | Good (PostgreSQL default) |
| `REPEATABLE_READ` | Repeatable Read | Prevented | Prevented | Possible* | Moderate (MySQL InnoDB default) |
| `SERIALIZABLE` | Serializable | Prevented | Prevented | Prevented | Slowest |

*MySQL InnoDB prevents phantom reads at REPEATABLE_READ via gap locks, but the SQL standard says they are possible.

```java
@Service
public class BalanceService {

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal calculateRunningBalance(UUID accountId) {
        // Multiple reads within this TX see a consistent snapshot
        BigDecimal debits = transactionRepository.sumDebits(accountId);
        BigDecimal credits = transactionRepository.sumCredits(accountId);
        return credits.subtract(debits);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferWithSerializableIsolation(UUID fromId, UUID toId, BigDecimal amount) {
        // Full serialization — prevents all anomalies but may cause deadlocks
        // Use only when absolute correctness is required
    }
}
```

**Important caveats:**
- Not all databases support all isolation levels. `READ_UNCOMMITTED` is effectively `READ_COMMITTED` in PostgreSQL.
- Changing isolation mid-connection may not work with all connection pools. Prefer setting it at the `@Transactional` annotation level.
- Higher isolation levels increase the chance of **deadlocks** and **lock contention**. Use the minimum level that satisfies your consistency requirements.
- For concurrency topics, see also [TopJavaConcurrencyInterviewQuestions.md](TopJavaConcurrencyInterviewQuestions.md).

---

### **Q59: What is a read-only transaction and how does Spring optimize it?**

Setting `@Transactional(readOnly = true)` provides **optimization hints** to both the Spring framework and the underlying persistence provider.

```java
@Service
@Transactional(readOnly = true)  // Class-level default
public class OrderQueryService {

    public Optional<OrderDto> findById(UUID id) {
        return orderRepository.findById(id).map(orderMapper::toDto);
    }

    public Page<OrderDto> search(OrderSearchCriteria criteria, Pageable pageable) {
        return orderRepository.findAll(toSpec(criteria), pageable)
                .map(orderMapper::toDto);
    }

    @Transactional // Overrides class-level readOnly for this mutation
    public void updateStatus(UUID id, OrderStatus status) {
        orderRepository.updateStatus(id, status);
    }
}
```

#### What `readOnly = true` Does

| Layer | **Optimization** |
|---|---|
| **Spring TX Manager** | Sets `Connection.setReadOnly(true)` on the JDBC connection |
| **JDBC Driver** | May route to a **read replica** (e.g., AWS RDS Proxy, PgPool) |
| **Hibernate** | Sets `FlushMode` to `MANUAL` -- **no dirty checking** at flush time |
| **Hibernate** | Skips taking **entity snapshots** in the persistence context (less memory) |
| **Database** | May use read-only transaction optimizations (e.g., PostgreSQL can skip WAL writes for read-only TX) |

**Performance impact:**
- Dirty checking iterates over all managed entities and compares each field. For a query returning 1,000 entities, skipping this step saves significant CPU.
- No entity snapshots means roughly **50% less memory** in the persistence context for loaded entities.

**Caveat:** `readOnly = true` does **not** prevent writes at the database level. If your code calls `save()` or `flush()`, Hibernate will skip the flush in `MANUAL` mode, and changes will be silently lost. Some databases will reject write operations on a read-only connection.

---

### **Q60: What are the most common @Transactional pitfalls?**

| # | **Pitfall** | **What Happens** | **Fix** |
|---|---|---|---|
| 1 | **Private/protected method** | `@Transactional` is ignored -- CGLIB proxies only intercept `public` methods | Make the method `public` |
| 2 | **Self-invocation** | Internal `this.method()` call bypasses the proxy -- no TX management | Extract to a separate bean (see Q56) |
| 3 | **Checked exceptions** | By default, `@Transactional` **does not rollback** on checked exceptions | Use `@Transactional(rollbackFor = Exception.class)` |
| 4 | **Catching exceptions silently** | Exception is swallowed inside `@Transactional` method; Spring thinks it succeeded and commits | Re-throw, or manually call `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` |
| 5 | **Missing `@EnableTransactionManagement`** | TX management not active (Spring Boot auto-configures this, so rare in Boot apps) | Ensure Boot's auto-configuration is not excluded |
| 6 | **Wrong `@Transactional` import** | Using `javax.transaction.Transactional` (JTA) instead of `org.springframework.transaction.annotation.Transactional` | Use the Spring annotation -- it supports more attributes |
| 7 | **Long-running transactions** | Holding a DB connection + locks for too long (calling external APIs inside a TX) | Move external calls **outside** the TX boundary |
| 8 | **`readOnly = true` with mutations** | Writes are silently discarded (Hibernate `FlushMode.MANUAL`) | Remove `readOnly` or override at method level |
| 9 | **REQUIRES_NEW in same class** | Self-invocation (pitfall #2) prevents new TX from being created | Extract to a separate bean |
| 10 | **No rollback for `Error`** | By default, Spring rolls back for `RuntimeException` and `Error`, but not `Throwable` | Use `rollbackFor = Throwable.class` if needed |

```java
// Pitfall #3: Checked exception — no rollback by default!
@Transactional
public void processPayment(UUID orderId) throws PaymentException {
    // If PaymentException (checked) is thrown, TX COMMITS — data may be inconsistent
    paymentGateway.charge(orderId); // throws PaymentException
}

// Fix: Explicitly declare rollbackFor
@Transactional(rollbackFor = PaymentException.class)
public void processPayment(UUID orderId) throws PaymentException {
    paymentGateway.charge(orderId);
}

// Pitfall #4: Catching and swallowing
@Transactional
public void riskyOperation() {
    try {
        repository.save(entity);
        externalService.notify(); // throws RuntimeException
    } catch (Exception e) {
        log.error("Failed", e); // TX commits! Partial data persisted.
    }
}

// Pitfall #7: Long-running TX holding a DB connection
@Transactional // BAD — holds connection during HTTP call
public void processOrder(Order order) {
    orderRepository.save(order);
    externalApi.notifyWarehouse(order); // 2-second HTTP call while holding connection + locks
    order.setStatus(NOTIFIED);
    orderRepository.save(order);
}

// Fix: Separate the TX from the external call
public void processOrder(Order order) {
    saveOrder(order);                         // TX #1
    externalApi.notifyWarehouse(order);       // No TX — no connection held
    markAsNotified(order.getId());            // TX #2
}

@Transactional
public void saveOrder(Order order) { orderRepository.save(order); }

@Transactional
public void markAsNotified(UUID orderId) {
    orderRepository.updateStatus(orderId, NOTIFIED);
}
```

---

## Section 12: Testing in Spring Boot

---

### **Q61: What are Spring Boot test slice annotations? When should you use each?**

Test slice annotations load **only a subset** of the application context, making tests faster and more focused.

| Annotation | **What It Loads** | **Auto-Configures** | **Use Case** |
|---|---|---|---|
| `@WebMvcTest` | `@Controller`, `@ControllerAdvice`, filters, `WebMvcConfigurer` | MockMvc, Jackson, validation | Controller / web layer tests |
| `@DataJpaTest` | `@Entity`, `@Repository`, JPA infrastructure | Embedded DB (H2), `TestEntityManager`, Flyway/Liquibase | Repository / query tests |
| `@DataJdbcTest` | JDBC repositories, `JdbcTemplate` | Embedded DB, Flyway/Liquibase | Spring Data JDBC tests |
| `@JdbcTest` | `JdbcTemplate`, `DataSource` | Embedded DB | Raw JDBC / `JdbcTemplate` tests |
| `@JsonTest` | `ObjectMapper`, `@JsonComponent` | Jackson, Gson, Jsonb | JSON serialization / deserialization tests |
| `@RestClientTest` | `RestClient`, `RestTemplate` builders | `MockRestServiceServer` | REST client tests (mocking external APIs) |
| `@SpringBootTest` | **Entire** application context | Everything | Full integration tests |

```java
// @WebMvcTest — only loads the web layer
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService; // Service layer is mocked

    @Test
    void shouldReturnOrder() throws Exception {
        // ...test controller in isolation
    }
}

// @DataJpaTest — only loads JPA components
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldFindByStatus() {
        // Uses embedded H2 by default, transactional + auto-rollback
    }
}

// @SpringBootTest — full context (use sparingly)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndRetrieveOrder() {
        // Full end-to-end test with real beans
    }
}
```

**Rule of thumb:** Start with the narrowest slice that covers your test case. Only escalate to `@SpringBootTest` when you need multiple layers working together.

---

### **Q62: How do you test controllers with MockMvc?**

`MockMvc` performs HTTP requests against your controller layer **without starting a real HTTP server**. It is fast, deterministic, and provides a fluent assertion API.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void shouldReturnOrderById() throws Exception {
        var order = new OrderDto(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                OrderStatus.ACTIVE,
                new BigDecimal("99.99"),
                Instant.parse("2024-01-15T10:30:00Z"));

        when(orderService.findById(order.id())).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/orders/{id}", order.id())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(order.id().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalAmount").value(99.99));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order Not Found"));
    }

    @Test
    void shouldCreateOrder() throws Exception {
        var request = new CreateOrderRequest(UUID.randomUUID(), List.of(
                new OrderItemRequest(UUID.randomUUID(), 2)));
        var created = new OrderDto(UUID.randomUUID(), OrderStatus.ACTIVE,
                new BigDecimal("49.98"), Instant.now());

        when(orderService.create(any(CreateOrderRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        var invalidRequest = new CreateOrderRequest(null, List.of()); // Missing customerId

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
```

**Key `MockMvc` methods:**

| Method | **Purpose** |
|---|---|
| `perform(get(...))` | Execute an HTTP request |
| `andExpect(status().isOk())` | Assert HTTP status code |
| `andExpect(jsonPath("$.field"))` | Assert JSON response body using JsonPath |
| `andExpect(header().exists("X-Foo"))` | Assert response headers |
| `andExpect(content().contentType(...))` | Assert content type |
| `andDo(print())` | Print request/response details for debugging |

---

### **Q63: What is the difference between @MockBean, @SpyBean, and Mockito mocks?**

| Aspect | **`@MockBean`** | **`@SpyBean`** | **`@Mock` (Mockito)** |
|---|---|---|---|
| **Source** | Spring Boot Test | Spring Boot Test | Mockito |
| **Context** | Replaces bean in Spring `ApplicationContext` | Wraps the real bean in the `ApplicationContext` with spy behavior | Pure Mockito -- no Spring context involved |
| **Behavior** | All methods return defaults (null, 0, false) unless stubbed | **Real methods execute** unless explicitly stubbed | All methods return defaults unless stubbed |
| **Use with** | `@WebMvcTest`, `@SpringBootTest` | `@WebMvcTest`, `@SpringBootTest` | `@ExtendWith(MockitoExtension.class)` -- no Spring context |
| **Context caching** | **Breaks** context caching (new context per unique mock combo) | **Breaks** context caching | Does not affect Spring context |
| **Performance** | Slower (Spring context reload) | Slower (Spring context reload) | **Fast** (no context) |
| **Best for** | Replacing a dependency in a slice test | Verifying interactions while keeping real behavior | Unit tests without Spring |

```java
// @Mock — pure unit test (fastest)
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldPlaceOrder() {
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentService.charge(any())).thenReturn(PaymentResult.SUCCESS);

        orderService.placeOrder(new OrderRequest(...));

        verify(paymentService).charge(any());
        verify(orderRepository).save(any());
    }
}

// @MockBean — replaces bean in Spring context
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @MockBean
    private OrderService orderService; // Real OrderService is replaced

    @Autowired
    private MockMvc mockMvc;
}

// @SpyBean — wraps real bean, allows selective stubbing
@SpringBootTest
class NotificationIntegrationTest {

    @SpyBean
    private EmailService emailService; // Real bean, but we can verify and stub

    @Test
    void shouldSendEmailOnOrderPlaced() {
        // Real method executes, but we verify it was called
        orderService.placeOrder(request);
        verify(emailService).sendOrderConfirmation(any());
    }

    @Test
    void shouldHandleEmailFailureGracefully() {
        // Stub only this specific scenario
        doThrow(new EmailException("SMTP down"))
                .when(emailService).sendOrderConfirmation(any());

        assertDoesNotThrow(() -> orderService.placeOrder(request));
    }
}
```

**Best practice:** Prefer `@Mock` (plain Mockito) for unit tests. Reserve `@MockBean` / `@SpyBean` for slice or integration tests where you need a real Spring context but want to isolate one dependency.

---

### **Q64: How do you use Testcontainers for integration tests with a real database?**

**Testcontainers** provides lightweight, disposable Docker containers for integration testing. This ensures your tests run against the **same database engine** as production, avoiding H2 compatibility issues.

```java
@SpringBootTest
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection  // Spring Boot 3.1+ — auto-configures DataSource from container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistAndRetrieveOrder() {
        var order = new Order(UUID.randomUUID(), OrderStatus.ACTIVE, new BigDecimal("99.99"));
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = orderRepository.findById(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo("99.99");
    }

    @Test
    void shouldExecuteNativeQueryWithPostgresFeatures() {
        // Test Postgres-specific features that H2 does not support
        List<Order> results = orderRepository.findWithRanking("ACTIVE");
        assertThat(results).isNotEmpty();
    }
}
```

**Reusable container configuration (abstract base class):**

```java
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withReuse(true); // Reuse container across test classes (faster)

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);
}

class OrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void shouldCreateOrder() {
        // Real PostgreSQL + real Redis
    }
}
```

**`@ServiceConnection` (Spring Boot 3.1+):** Automatically detects the container type and configures the appropriate connection properties (`spring.datasource.url`, `spring.redis.host`, etc.) -- no more manual `@DynamicPropertySource`.

**For legacy Spring Boot or non-standard containers:**

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

---

### **Q65: What are Spring Boot testing best practices?**

#### The Test Pyramid

```
        /   \
       / E2E \          Few — @SpringBootTest with real HTTP (TestRestTemplate/WebTestClient)
      /-------\
     /  Integ  \        Moderate — @DataJpaTest, @WebMvcTest, Testcontainers
    /-----------\
   /    Unit     \      Many — Plain JUnit + Mockito, no Spring context
  /_______________\
```

#### Best Practices

**1. Follow the test pyramid -- unit tests are the foundation**

```java
// GOOD: Pure unit test — no Spring context, runs in milliseconds
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock private DiscountRepository discountRepository;
    @InjectMocks private PricingService pricingService;

    @Test
    void shouldApplyPercentageDiscount() {
        when(discountRepository.findActive(any()))
                .thenReturn(Optional.of(new Discount(DiscountType.PERCENTAGE, new BigDecimal("10"))));

        BigDecimal result = pricingService.calculatePrice(new BigDecimal("100"), "PROMO10");

        assertThat(result).isEqualByComparingTo("90.00");
    }
}
```

**2. Avoid `@SpringBootTest` for everything**

| Problem | **Impact** |
|---|---|
| Loads the entire application context | Slow startup (seconds to minutes) |
| Breaks context caching with `@MockBean` | Even slower across test suite |
| Tests too many things at once | Flaky, hard to diagnose failures |

**3. Use test slices for focused testing**

```java
// Test ONLY the controller layer
@WebMvcTest(ProductController.class)
class ProductControllerTest { ... }

// Test ONLY the repository layer
@DataJpaTest
class ProductRepositoryTest { ... }

// Test ONLY JSON serialization
@JsonTest
class ProductDtoJsonTest {
    @Autowired
    private JacksonTester<ProductDto> json;

    @Test
    void shouldSerialize() throws Exception {
        var dto = new ProductDto(UUID.randomUUID(), "Widget", new BigDecimal("29.99"));
        assertThat(json.write(dto)).extractingJsonPathStringValue("$.name").isEqualTo("Widget");
    }
}
```

**4. Use Testcontainers for database tests, not H2**

H2 behaves differently from PostgreSQL/MySQL in subtle ways (JSON operators, window functions, sequence behavior, locking). Use Testcontainers to test against the real database engine.

**5. Name tests clearly with the should/when pattern**

```java
@Test
void shouldRejectOrderWhenInsufficientInventory() { ... }

@Test
void shouldRetry3TimesWhenPaymentGatewayTimesOut() { ... }
```

**6. Use `@Transactional` on `@DataJpaTest` for auto-rollback**

`@DataJpaTest` is `@Transactional` by default -- each test rolls back automatically. This keeps tests isolated without manual cleanup.

**7. Avoid testing framework behavior**

```java
// BAD: Tests that Spring's @Valid works (framework test)
@Test
void shouldRejectNullName() {
    // Just testing that Jakarta validation annotations work — not your code
}

// GOOD: Tests YOUR validation logic
@Test
void shouldRejectOrderWhenItemQuantityExceedsStockLevel() {
    // Tests actual business validation rule
}
```

**8. Use `@DirtiesContext` sparingly**

`@DirtiesContext` forces the application context to be rebuilt. Use it only when a test truly mutates shared state that cannot be rolled back (e.g., modifying a singleton bean's internal state).

**9. Parallelize tests safely**

```properties
# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
```

Ensure tests do not share mutable state. Use unique test data (random UUIDs, unique emails) to avoid collisions.

Here is the complete PART II content covering all 33 questions (Q33 through Q65) across the 6 sections. The content follows the exact style conventions from the existing `TopJavaConcurrencyInterviewQuestions.md` document in your repository:

- **H3 bold question format**: `### **Q#: Question?**`
- **Fenced code blocks** with `java` language tags
- **Pipe-syntax comparison tables** where specified
- **Horizontal rules** (`---`) between questions
- **Spring Boot 3.x / Java 17+** syntax throughout
- **Cross-references** to `[Section 15](#15-transactions--isolation-levels)` and `[TopJavaConcurrencyInterviewQuestions.md](TopJavaConcurrencyInterviewQuestions.md)`

Key highlights of the content:

- **Section 7 (IoC/DI/Lifecycle)** covers constructor vs setter vs field injection with a comparison table, all 5 bean scopes, the full 12-step bean lifecycle flow, stereotype annotation differences (including `@Repository` exception translation), `@Configuration` full mode vs lite mode (with `proxyBeanMethods = false`), and circular dependency solutions.

- **Section 8 (Auto-Configuration)** explains the Boot 3.x `AutoConfiguration.imports` file mechanism, the complete 12-level property precedence table, `@Value` vs `@ConfigurationProperties` with immutable records, profile groups and expressions, and custom auto-configuration with `ApplicationContextRunner` testing.

- **Section 9 (Spring MVC/REST)** covers `@RestController` vs `@Controller`, all request mapping annotations with realistic code, RFC 7807 `ProblemDetail` exception handling (Spring Boot 3.x native), validation groups and custom validators, HTTP method/status code tables, and the Filter/Interceptor/AOP request pipeline comparison.

- **Section 10 (Spring Data JPA)** covers the full repository hierarchy table, entity mapping with `@GeneratedValue` strategy comparison, bidirectional relationships with convenience methods, JPQL/native/Criteria/Specification comparison with composable Specification examples, lazy loading solutions (`JOIN FETCH`, `@EntityGraph`, DTO projection), and `Page` vs `Slice` vs keyset pagination (Spring Data 3.1+).

- **Section 11 (Transactions)** explains proxy-based AOP mechanics, the self-invocation problem with 3 solutions, all 7 propagation levels with `NESTED` vs `REQUIRES_NEW` distinction, isolation level mapping table, `readOnly = true` optimizations, and a comprehensive 10-row pitfalls table with code examples.

- **Section 12 (Testing)** covers all test slice annotations in a comparison table, a full `MockMvc` test class with 4 test methods, `@MockBean`/`@SpyBean`/`@Mock` differences, Testcontainers with `@ServiceConnection` (Boot 3.1+), and 9 concrete testing best practices including the test pyramid.



## PART III — DATABASES

---

## 13. SQL Fundamentals & Joins

---

### **Q66: What are the SQL JOIN types and when do you use each?**

A **JOIN** combines rows from two or more tables based on a related column.

**ASCII Venn Diagrams:**

```
INNER JOIN          LEFT JOIN           RIGHT JOIN          FULL OUTER JOIN
  ┌───┬───┬───┐      ┌───┬───┬───┐      ┌───┬───┬───┐      ┌───┬───┬───┐
  │ A │A∩B│ B │      │ A │A∩B│ B │      │ A │A∩B│ B │      │ A │A∩B│ B │
  │   │███│   │      │███│███│   │      │   │███│███│      │███│███│███│
  └───┴───┴───┘      └───┴───┴───┘      └───┴───┴───┘      └───┴───┴───┘
  Only matching       All left +         All right +        All rows from
  rows from both      matching right     matching left      both tables

CROSS JOIN
  Every row in A × every row in B (Cartesian product)
```

| JOIN Type | Returns | NULL Behaviour | Use Case |
|---|---|---|---|
| `INNER JOIN` | Only rows with matches in **both** tables | No NULLs from join | Default; fetch related data that must exist |
| `LEFT JOIN` | **All** left rows + matched right rows | Right columns are `NULL` when no match | "Show all customers, even those with no orders" |
| `RIGHT JOIN` | **All** right rows + matched left rows | Left columns are `NULL` when no match | Rarely used; rewrite as `LEFT JOIN` with tables swapped |
| `FULL OUTER JOIN` | **All** rows from both tables | NULLs on either side when no match | Reconciliation queries, finding unmatched rows on both sides |
| `CROSS JOIN` | Cartesian product (M × N rows) | No NULLs | Generate combinations (e.g., sizes × colours), calendar generation |
| `SELF JOIN` | Table joined to itself | Depends on join type used | Hierarchical data, comparing rows within the same table |

**Examples:**

```sql
-- INNER JOIN: customers who have placed orders
SELECT c.name, o.order_date, o.total
FROM   customers c
INNER JOIN orders o ON o.customer_id = c.id;

-- LEFT JOIN: all customers, orders may be NULL
SELECT c.name, o.order_date, o.total
FROM   customers c
LEFT JOIN orders o ON o.customer_id = c.id;

-- FULL OUTER JOIN: find unmatched rows on both sides
SELECT c.name, o.id AS order_id
FROM   customers c
FULL OUTER JOIN orders o ON o.customer_id = c.id
WHERE  c.id IS NULL OR o.id IS NULL;

-- CROSS JOIN: generate all product-warehouse combinations
SELECT p.name, w.location
FROM   products p
CROSS JOIN warehouses w;

-- SELF JOIN: find employees and their managers
SELECT e.name AS employee, m.name AS manager
FROM   employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

**Performance note:** `INNER JOIN` gives the optimizer the most freedom to reorder tables. `LEFT JOIN` constrains ordering since the left table must be preserved. Always prefer `INNER JOIN` when the relationship is guaranteed.

---

### **Q67: Explain GROUP BY, HAVING, and aggregate functions.**

**GROUP BY** collapses rows sharing the same values into summary rows. **HAVING** filters groups (as opposed to `WHERE`, which filters individual rows before aggregation).

**SQL logical processing order:**

```
FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
         ↑                    ↑
    filters rows       filters groups
```

**Core aggregate functions:**

| Function | Description | NULL Handling |
|---|---|---|
| `COUNT(*)` | Count all rows in group | Counts rows with NULLs |
| `COUNT(col)` | Count non-NULL values | Ignores NULLs |
| `COUNT(DISTINCT col)` | Count unique non-NULL values | Ignores NULLs |
| `SUM(col)` | Sum of values | Ignores NULLs |
| `AVG(col)` | Average of values | Ignores NULLs (does **not** treat NULL as 0) |
| `MIN(col)` / `MAX(col)` | Minimum / maximum value | Ignores NULLs |
| `BOOL_AND(col)` / `BOOL_OR(col)` | Logical AND / OR (PostgreSQL) | Ignores NULLs |
| `STRING_AGG(col, ',')` | Concatenate strings (PostgreSQL) | Ignores NULLs |
| `ARRAY_AGG(col)` | Collect into array (PostgreSQL) | Includes NULLs by default |

**Examples:**

```sql
-- Revenue by category, only categories with > $10k revenue
SELECT   p.category,
         COUNT(*)            AS order_count,
         SUM(oi.quantity)    AS units_sold,
         SUM(oi.line_total)  AS revenue,
         AVG(oi.line_total)  AS avg_order_value
FROM     order_items oi
JOIN     products p ON p.id = oi.product_id
GROUP BY p.category
HAVING   SUM(oi.line_total) > 10000
ORDER BY revenue DESC;

-- Count distinct customers per month
SELECT   DATE_TRUNC('month', o.order_date) AS month,
         COUNT(DISTINCT o.customer_id)      AS unique_customers,
         COUNT(*)                            AS total_orders
FROM     orders o
WHERE    o.order_date >= '2024-01-01'
GROUP BY DATE_TRUNC('month', o.order_date)
ORDER BY month;

-- Multiple aggregates with FILTER (PostgreSQL 9.4+)
SELECT   department_id,
         COUNT(*)                                 AS total,
         COUNT(*) FILTER (WHERE salary > 100000)  AS high_earners,
         AVG(salary) FILTER (WHERE tenure_years > 5) AS avg_salary_senior
FROM     employees
GROUP BY department_id;
```

**Common pitfalls:**
- Selecting a non-aggregated column that is not in `GROUP BY` — this is an error in standard SQL and PostgreSQL (MySQL historically allowed it with unpredictable results).
- Using `WHERE` instead of `HAVING` to filter on aggregates — `WHERE SUM(x) > 10` is a syntax error.
- Forgetting that `AVG` ignores NULLs: `AVG(COALESCE(col, 0))` if you need NULLs treated as zero.

---

### **Q68: Subqueries vs JOINs — when should you prefer one over the other?**

**Correlated vs non-correlated subqueries:**

| Type | Definition | Execution | Example |
|---|---|---|---|
| **Non-correlated** | Subquery is independent, runs **once** | Result cached, used by outer query | `WHERE id IN (SELECT id FROM ...)` |
| **Correlated** | Subquery references the **outer** query | Runs **once per outer row** (conceptually) | `WHERE salary > (SELECT AVG(salary) FROM employees e2 WHERE e2.dept = e1.dept)` |

**Non-correlated subquery vs JOIN:**

```sql
-- Subquery: find customers who ordered in 2024
SELECT c.name
FROM   customers c
WHERE  c.id IN (
    SELECT o.customer_id
    FROM   orders o
    WHERE  o.order_date >= '2024-01-01'
);

-- Equivalent JOIN (may need DISTINCT)
SELECT DISTINCT c.name
FROM   customers c
JOIN   orders o ON o.customer_id = c.id
WHERE  o.order_date >= '2024-01-01';

-- EXISTS (often faster than IN for large subquery results)
SELECT c.name
FROM   customers c
WHERE  EXISTS (
    SELECT 1 FROM orders o
    WHERE  o.customer_id = c.id
      AND  o.order_date >= '2024-01-01'
);
```

**Correlated subquery example:**

```sql
-- Employees earning above their department average
SELECT e.name, e.salary, e.department_id
FROM   employees e
WHERE  e.salary > (
    SELECT AVG(e2.salary)
    FROM   employees e2
    WHERE  e2.department_id = e.department_id
);

-- Rewritten with JOIN (generally faster for large tables)
SELECT e.name, e.salary, e.department_id
FROM   employees e
JOIN   (
    SELECT department_id, AVG(salary) AS avg_sal
    FROM   employees
    GROUP BY department_id
) dept_avg ON dept_avg.department_id = e.department_id
WHERE  e.salary > dept_avg.avg_sal;
```

**Decision guide:**

| Scenario | Recommendation | Reason |
|---|---|---|
| Existence check | `EXISTS` subquery | Stops at first match; no duplicate concern |
| Filter by value list | `IN` subquery (small list) or `JOIN` (large) | `IN` with thousands of values degrades |
| Need columns from both tables | `JOIN` | Subquery cannot produce extra columns in outer SELECT |
| Derived/aggregated dataset | Subquery in `FROM` (derived table) or CTE | Clear separation of logic |
| Correlated with complex logic | Rewrite as `JOIN` or window function | Avoids repeated subquery execution |
| Readability matters most | Whichever is clearer | Modern optimizers often produce identical plans |

**Performance note:** PostgreSQL's optimizer can often flatten `IN` subqueries into semi-joins, and `EXISTS` into semi-joins. Check `EXPLAIN ANALYZE` — the plan may be identical. When in doubt, benchmark both (see [Section 14](#14-indexes--query-optimization)).

---

### **Q69: What are window functions and how do you use them?**

A **window function** performs a calculation across a set of rows related to the current row — without collapsing rows like `GROUP BY`. You define the "window" with `OVER(PARTITION BY ... ORDER BY ...)`.

**Syntax:**

```sql
function_name(...) OVER (
    [PARTITION BY col1, col2]       -- divide rows into partitions
    [ORDER BY col3 [ASC|DESC]]      -- order within each partition
    [ROWS|RANGE BETWEEN ... AND ...] -- frame specification
)
```

**Common window functions:**

| Function | Description |
|---|---|
| `ROW_NUMBER()` | Unique sequential integer per partition (no ties) |
| `RANK()` | Rank with gaps (1, 2, 2, **4**) |
| `DENSE_RANK()` | Rank without gaps (1, 2, 2, **3**) |
| `NTILE(n)` | Distribute rows into `n` roughly equal buckets |
| `LAG(col, n)` | Value from `n` rows **before** the current row |
| `LEAD(col, n)` | Value from `n` rows **after** the current row |
| `FIRST_VALUE(col)` | First value in the window frame |
| `LAST_VALUE(col)` | Last value in the window frame |
| `SUM/AVG/COUNT(col) OVER(...)` | Running or sliding aggregate |

**Examples:**

```sql
-- ROW_NUMBER, RANK, DENSE_RANK comparison
SELECT name,
       department,
       salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC) AS row_num,
       RANK()       OVER (ORDER BY salary DESC) AS rank,
       DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rank
FROM   employees;

-- Result (ties at salary 90000):
-- name    | salary | row_num | rank | dense_rank
-- Alice   | 120000 |    1    |   1  |     1
-- Bob     |  90000 |    2    |   2  |     2
-- Charlie |  90000 |    3    |   2  |     2
-- Diana   |  80000 |    4    |   4  |     3    ← rank skips, dense_rank doesn't
```

```sql
-- Top-N per group: highest-paid employee per department
SELECT *
FROM (
    SELECT name, department, salary,
           ROW_NUMBER() OVER (
               PARTITION BY department
               ORDER BY salary DESC
           ) AS rn
    FROM employees
) ranked
WHERE rn = 1;
```

```sql
-- LAG/LEAD: month-over-month revenue change
SELECT month,
       revenue,
       LAG(revenue, 1)  OVER (ORDER BY month)  AS prev_month_revenue,
       revenue - LAG(revenue, 1) OVER (ORDER BY month) AS mom_change,
       ROUND(
           (revenue - LAG(revenue, 1) OVER (ORDER BY month))
           * 100.0 / LAG(revenue, 1) OVER (ORDER BY month), 2
       ) AS mom_change_pct
FROM   monthly_revenue;
```

```sql
-- Running total and moving average
SELECT order_date,
       daily_revenue,
       SUM(daily_revenue) OVER (
           ORDER BY order_date
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS running_total,
       AVG(daily_revenue) OVER (
           ORDER BY order_date
           ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       ) AS seven_day_moving_avg
FROM   daily_sales;
```

```sql
-- Percentage of total per department
SELECT name,
       department,
       salary,
       ROUND(salary * 100.0 / SUM(salary) OVER (PARTITION BY department), 2)
           AS pct_of_dept_total,
       ROUND(salary * 100.0 / SUM(salary) OVER (), 2)
           AS pct_of_company_total
FROM   employees;
```

**Frame specification cheat sheet:**

| Frame Clause | Meaning |
|---|---|
| `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` | All rows from start to current (default with `ORDER BY`) |
| `ROWS BETWEEN 3 PRECEDING AND CURRENT ROW` | Sliding window of last 4 rows |
| `ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING` | Current row ± 1 neighbour |
| `ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING` | Entire partition |

---

### **Q70: What are Common Table Expressions (CTEs) and recursive CTEs?**

A **CTE** (the `WITH` clause) defines a named temporary result set scoped to a single statement. Think of it as an inline view that improves readability and allows reuse within the query.

**Basic CTE:**

```sql
WITH active_customers AS (
    SELECT c.id, c.name, c.email
    FROM   customers c
    WHERE  c.status = 'ACTIVE'
      AND  EXISTS (
          SELECT 1 FROM orders o
          WHERE  o.customer_id = c.id
            AND  o.order_date >= CURRENT_DATE - INTERVAL '90 days'
      )
),
customer_spend AS (
    SELECT ac.id,
           ac.name,
           SUM(o.total)   AS total_spend,
           COUNT(o.id)    AS order_count
    FROM   active_customers ac
    JOIN   orders o ON o.customer_id = ac.id
    GROUP BY ac.id, ac.name
)
SELECT name, total_spend, order_count,
       RANK() OVER (ORDER BY total_spend DESC) AS spend_rank
FROM   customer_spend
WHERE  order_count >= 3
ORDER BY total_spend DESC;
```

**Key points:**
- CTEs are referenced by name — you can chain multiple CTEs with commas.
- Each CTE can reference previously defined CTEs.
- PostgreSQL 12+ can **inline** non-recursive CTEs (before 12 they were optimization fences).
- To force materialization: `WITH cte AS MATERIALIZED (...)` (PostgreSQL 12+).
- To force inlining: `WITH cte AS NOT MATERIALIZED (...)`.

**Recursive CTE:**

A recursive CTE references itself. It has two parts:
1. **Anchor member** — the base case (non-recursive).
2. **Recursive member** — references the CTE itself, iterates until no new rows are produced.

```sql
-- Organizational hierarchy: find all reports under a manager
WITH RECURSIVE org_tree AS (
    -- Anchor: start with a specific manager
    SELECT id, name, manager_id, 1 AS depth, 
           ARRAY[name]::TEXT[] AS path
    FROM   employees
    WHERE  id = 100   -- starting manager

    UNION ALL

    -- Recursive: find direct reports of each person already found
    SELECT e.id, e.name, e.manager_id, ot.depth + 1,
           ot.path || e.name
    FROM   employees e
    JOIN   org_tree ot ON e.manager_id = ot.id
    WHERE  ot.depth < 10  -- safety limit to avoid infinite loops
)
SELECT id, name, depth, 
       ARRAY_TO_STRING(path, ' → ') AS reporting_chain
FROM   org_tree
ORDER BY path;
```

```sql
-- Generate a date series (alternative to generate_series)
WITH RECURSIVE date_range AS (
    SELECT DATE '2024-01-01' AS dt
    UNION ALL
    SELECT dt + INTERVAL '1 day'
    FROM   date_range
    WHERE  dt < DATE '2024-12-31'
)
SELECT dt
FROM   date_range;

-- Bill of materials: find all components of a product (recursive)
WITH RECURSIVE bom AS (
    SELECT component_id, part_name, quantity, 1 AS level
    FROM   bill_of_materials
    WHERE  parent_product_id = 42

    UNION ALL

    SELECT b.component_id, b.part_name, 
           b.quantity * bom.quantity AS quantity,
           bom.level + 1
    FROM   bill_of_materials b
    JOIN   bom ON b.parent_product_id = bom.component_id
    WHERE  bom.level < 20
)
SELECT component_id, part_name, quantity, level
FROM   bom
ORDER BY level, part_name;
```

**CTE vs subquery vs temp table:**

| Feature | CTE | Subquery | Temp Table |
|---|---|---|---|
| Readability | Best — named, ordered | Inline, can be nested deeply | Separate statement |
| Reuse within query | Yes, reference by name | Must duplicate | Yes |
| Recursion | Yes (`WITH RECURSIVE`) | No | Manual loop with PL/pgSQL |
| Materialization (PG 12+) | Controlled per CTE | Always inlined | Always materialized |
| Scope | Single statement | Single use | Entire session/transaction |
| Indexable | No | No | Yes |

---

## 14. Indexes & Query Optimization

---

### **Q71: How do B-tree indexes work?**

A **B-tree** (balanced tree) is the default index type in PostgreSQL (and most relational databases). It maintains sorted data in a self-balancing tree structure that supports efficient equality and range lookups.

**Structure:**

```
                        ┌──────────────────────┐
                        │  Root Node           │
                        │  [30 | 60]           │
                        └──┬────┬────┬─────────┘
                           │    │    │
             ┌─────────────┘    │    └─────────────┐
             ▼                  ▼                   ▼
    ┌────────────┐    ┌────────────┐      ┌────────────┐
    │ Internal   │    │ Internal   │      │ Internal   │
    │ [10 | 20]  │    │ [40 | 50]  │      │ [70 | 80]  │
    └─┬──┬──┬────┘    └─┬──┬──┬────┘      └─┬──┬──┬────┘
      │  │  │            │  │  │              │  │  │
      ▼  ▼  ▼            ▼  ▼  ▼              ▼  ▼  ▼
    ┌──┐┌──┐┌──┐      ┌──┐┌──┐┌──┐        ┌──┐┌──┐┌──┐
    │  ││  ││  │      │  ││  ││  │        │  ││  ││  │
    │L1││L2││L3│ ←──→ │L4││L5││L6│  ←──→  │L7││L8││L9│
    └──┘└──┘└──┘      └──┘└──┘└──┘        └──┘└──┘└──┘
     Leaf nodes linked together for efficient range scans
     Each leaf stores (indexed value → heap tuple pointer)
```

**Key properties:**

| Property | Detail |
|---|---|
| **Search** | O(log n) — traverse root → internal → leaf |
| **Range scan** | Follow leaf node pointers (doubly linked list) |
| **Insert/Delete** | O(log n) — may trigger page split/merge |
| **Sorted order** | Data in leaves is sorted, enabling `ORDER BY` without extra sort |
| **Equality** | Traverse tree to single leaf |
| **Operators supported** | `<`, `<=`, `=`, `>=`, `>`, `BETWEEN`, `IN`, `IS NULL` |

**How a lookup works (searching for `salary = 50`):**
1. Start at root node `[30 | 60]` — 50 is between 30 and 60, follow middle pointer.
2. Arrive at internal node `[40 | 50]` — 50 matches, follow right pointer.
3. Arrive at leaf node — find the entry for 50, which contains a pointer (ctid) to the heap tuple on disk.
4. Fetch the row from the heap page.

**Range scan (`salary BETWEEN 40 AND 70`):**
1. Traverse tree to find 40 in a leaf node.
2. Scan right along the linked leaf nodes until a value > 70 is encountered.
3. Collect all matching heap tuple pointers.

**Why it matters for interviews:** Understanding B-tree structure explains *why* certain queries use indexes and others don't — for example, a `LIKE '%foo'` cannot use a B-tree because the leading characters are unknown, so there is no tree path to follow.

---

### **Q72: What are the different types of indexes?**

| Index Type | Description | Use Case | PostgreSQL Syntax |
|---|---|---|---|
| **Primary Key** | Unique, not null; one per table; B-tree | Row identity | `PRIMARY KEY (id)` |
| **Unique** | Enforces uniqueness; allows one NULL (PG) | Business constraints (email, SSN) | `CREATE UNIQUE INDEX idx ON t(email)` |
| **Composite** | Index on multiple columns | Multi-column WHERE/ORDER BY | `CREATE INDEX idx ON t(a, b, c)` |
| **Covering** | Includes extra columns to satisfy SELECT without heap access | Avoiding heap fetches for frequent queries | `CREATE INDEX idx ON t(a) INCLUDE (b, c)` |
| **Partial** | Index only a subset of rows | Sparse conditions (e.g., active records) | `CREATE INDEX idx ON t(email) WHERE status = 'ACTIVE'` |
| **Hash** | Hash-based equality lookup only | Exact equality (`=`), no range scans | `CREATE INDEX idx ON t USING hash(col)` |
| **GIN** | Generalized Inverted Index | Full-text search, arrays, JSONB containment | `CREATE INDEX idx ON t USING gin(tags)` |
| **GiST** | Generalized Search Tree | Geometric data, range types, full-text | `CREATE INDEX idx ON t USING gist(location)` |
| **BRIN** | Block Range Index | Very large, naturally ordered tables (time-series) | `CREATE INDEX idx ON t USING brin(created_at)` |
| **Expression** | Index on a function/expression result | Queries with function calls in WHERE | `CREATE INDEX idx ON t(LOWER(email))` |

**Examples:**

```sql
-- Composite index for a frequent query pattern
CREATE INDEX idx_orders_customer_date 
ON orders(customer_id, order_date DESC);

-- Covering index: avoids heap lookup for this query entirely
CREATE INDEX idx_orders_covering 
ON orders(customer_id, order_date) INCLUDE (total, status);

-- Partial index: only index active users (much smaller index)
CREATE INDEX idx_users_active_email 
ON users(email) WHERE status = 'ACTIVE';

-- Expression index: case-insensitive email lookup
CREATE INDEX idx_users_email_lower 
ON users(LOWER(email));

-- GIN index for JSONB containment queries
CREATE INDEX idx_products_attributes 
ON products USING gin(attributes jsonb_path_ops);

-- BRIN index for time-series data (very compact)
CREATE INDEX idx_events_created 
ON events USING brin(created_at) WITH (pages_per_range = 32);
```

**Index selection tip:** If the query is `SELECT a, b FROM t WHERE a = ? ORDER BY b`, a composite index on `(a, b)` provides both filtering and ordering — no extra sort step.

---

### **Q73: Explain composite index ordering and the leftmost prefix rule.**

A **composite index** on `(a, b, c)` stores data sorted by `a`, then by `b` within each `a`, then by `c` within each `(a, b)` pair. The **leftmost prefix rule** states that the index can only be used efficiently when the query's conditions match a contiguous left-to-right prefix of the index columns.

**Given index `CREATE INDEX idx ON orders(customer_id, status, order_date)`:**

| Query WHERE Clause | Index Used? | Reason |
|---|---|---|
| `customer_id = 5` | Yes | Leftmost column |
| `customer_id = 5 AND status = 'SHIPPED'` | Yes | Left two columns |
| `customer_id = 5 AND status = 'SHIPPED' AND order_date > '2024-01-01'` | Yes (full index) | All three columns |
| `status = 'SHIPPED'` | **No** (full scan) | Skipped `customer_id` |
| `order_date > '2024-01-01'` | **No** (full scan) | Skipped first two columns |
| `customer_id = 5 AND order_date > '2024-01-01'` | **Partial** | Uses `customer_id`, then scans within for `order_date` (skipped `status`) |

**Column order guidelines:**

1. **Equality columns first** — columns compared with `=` should be leftmost.
2. **Range/inequality column last** — columns compared with `>`, `<`, `BETWEEN` should be rightmost (only one range column can be efficiently used per index scan).
3. **High selectivity first** — among equality columns, the most selective (fewest matching rows) first is slightly better for skip-scan scenarios.

**Example — wrong vs right order:**

```sql
-- Query pattern:
-- WHERE status = ? AND customer_id = ? AND order_date BETWEEN ? AND ?

-- WRONG order: range column in the middle breaks the chain
CREATE INDEX idx_bad ON orders(status, order_date, customer_id);

-- RIGHT order: equalities first, range last
CREATE INDEX idx_good ON orders(status, customer_id, order_date);
```

**Sorting with composite indexes:**

```sql
-- The index ON orders(customer_id, order_date DESC) satisfies:
SELECT * FROM orders WHERE customer_id = 5 ORDER BY order_date DESC;
-- ✅ No extra sort needed — index provides both filter and order

-- But NOT:
SELECT * FROM orders WHERE customer_id = 5 ORDER BY order_date ASC;
-- PostgreSQL CAN backward-scan B-tree, so this actually works too.
-- However, mixed directions (e.g., ORDER BY a ASC, b DESC) need matching index direction.

CREATE INDEX idx_mixed ON orders(customer_id ASC, order_date DESC);
```

---

### **Q74: When do indexes hurt performance?**

Indexes are not free. Every index adds overhead to writes and consumes storage. Over-indexing is a common mistake.

**Write overhead:**

```
INSERT 1 row into a table with 5 indexes:
  1× heap insert  +  5× index inserts  =  6 write operations

UPDATE 1 indexed column in a table with 5 indexes:
  PostgreSQL HOT (Heap-Only Tuple) update if possible (no index change)
  Otherwise: dead tuple + new tuple + index updates for changed columns
  
DELETE: marks tuple as dead; VACUUM later reclaims space in heap + indexes
```

**Scenarios where indexes hurt or are useless:**

| Scenario | Why It Hurts | Solution |
|---|---|---|
| **Low cardinality** | Column with only 2-3 distinct values (e.g., `status` ENUM with 'ACTIVE'/'INACTIVE'); index scan reads most of the table anyway | Partial index (`WHERE status = 'ACTIVE'`) if one value is rare, otherwise skip the index |
| **Write-heavy tables** | Frequent INSERT/UPDATE/DELETE pays index maintenance cost on every write | Fewer, targeted indexes; consider batch inserts with indexes dropped then rebuilt |
| **Over-indexing** | Redundant indexes (e.g., `(a)` and `(a, b)` — the composite already covers `a`-only lookups) | Audit with `pg_stat_user_indexes` — drop unused indexes |
| **Small tables** | Seq Scan on 100 rows is faster than index lookup (sequential I/O vs random I/O) | PostgreSQL optimizer ignores the index automatically |
| **High UPDATE frequency on indexed column** | Each update creates new index entries; index bloat | HOT updates (keep updated columns out of indexes), `FILLFACTOR` |
| **Too-wide composite indexes** | Large index size, slower to traverse | Use `INCLUDE` columns instead of adding to key |

**Detecting unused indexes (PostgreSQL):**

```sql
SELECT schemaname, relname AS table, indexrelname AS index,
       idx_scan, idx_tup_read, idx_tup_fetch,
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM   pg_stat_user_indexes
WHERE  idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;
```

**Detecting duplicate/overlapping indexes:**

```sql
SELECT a.indexrelid::regclass AS index_a,
       b.indexrelid::regclass AS index_b,
       a.indkey, b.indkey
FROM   pg_index a
JOIN   pg_index b ON a.indrelid = b.indrelid
  AND  a.indexrelid <> b.indexrelid
  AND  a.indkey <@ b.indkey      -- a's columns are a subset of b's
WHERE  a.indrelid = 'orders'::regclass;
```

---

### **Q75: How do you read an EXPLAIN ANALYZE output?**

`EXPLAIN ANALYZE` runs the query and shows the **actual** execution plan with real timings and row counts.

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT c.name, COUNT(o.id) AS order_count
FROM   customers c
JOIN   orders o ON o.customer_id = c.id
WHERE  o.order_date >= '2024-01-01'
GROUP BY c.name
ORDER BY order_count DESC
LIMIT  10;
```

**Key plan nodes:**

| Node | Description | Performance Concern |
|---|---|---|
| **Seq Scan** | Full table scan, reads every row | Fine for small tables; bad for large tables with selective WHERE |
| **Index Scan** | Traverse index, then fetch heap rows | Good for selective queries; random I/O for heap fetches |
| **Index Only Scan** | Reads only the index (covering index) | Best case — no heap access |
| **Bitmap Index Scan** | Build bitmap of matching pages, then fetch | Good for moderate selectivity; batches random I/O |
| **Nested Loop** | For each outer row, scan inner table | Fast if inner side uses index; O(N×M) without |
| **Hash Join** | Build hash table from smaller side, probe with larger | Good for equality joins on large tables; needs memory |
| **Merge Join** | Merge two sorted inputs | Great when both sides are already sorted (index) |
| **Sort** | Sort rows in memory or on disk | Check `Sort Method: external merge` → spilling to disk |
| **HashAggregate** | GROUP BY using a hash table | Fast; watch for memory pressure |
| **GroupAggregate** | GROUP BY on pre-sorted input | No extra memory needed if input is sorted |
| **Materialize** | Cache a subplan's output | Reused when inner side of nested loop is scanned multiple times |

**Reading the output:**

```
Limit (cost=150.32..150.35 rows=10 width=40) (actual time=12.5..12.6 rows=10 loops=1)
  -> Sort (cost=150.32..152.00 rows=500 width=40) (actual time=12.5..12.5 rows=10 loops=1)
        Sort Key: (count(o.id)) DESC
        Sort Method: top-N heapsort  Memory: 26kB
        -> HashAggregate (cost=130.00..135.00 rows=500 ...) (actual time=11.2..11.8 rows=500 ...)
              Group Key: c.name
              -> Hash Join (cost=25.00..120.00 rows=2000 ...) (actual time=0.8..9.5 rows=2000 ...)
                    Hash Cond: (o.customer_id = c.id)
                    -> Bitmap Heap Scan on orders o (cost=5.00..90.00 ...) (actual ...)
                          Recheck Cond: (order_date >= '2024-01-01')
                          -> Bitmap Index Scan on idx_orders_date (...) (actual ...)
                    -> Hash (cost=15.00..15.00 rows=500 ...) (actual time=0.5..0.5 rows=500 ...)
                          -> Seq Scan on customers c (actual ...)
```

**What to look for:**

| Metric | What It Tells You |
|---|---|
| `actual time` vs `cost` | Estimated vs real — large discrepancies mean stale statistics (`ANALYZE`) |
| `rows` (estimated vs actual) | If estimated=1 but actual=100000, optimizer chose a bad plan |
| `loops` | How many times this node was executed (important for nested loops) |
| `Buffers: shared hit/read` | Cache hits vs disk reads — high `read` count means data isn't cached |
| `Sort Method: external merge Disk: XXkB` | Sort spilling to disk — increase `work_mem` |

---

### **Q76: What are essential query optimization techniques?**

**1. Avoid SELECT * — select only needed columns:**

```sql
-- BAD: fetches all columns, prevents index-only scans
SELECT * FROM orders WHERE customer_id = 5;

-- GOOD: only what you need, enables covering index
SELECT id, order_date, total FROM orders WHERE customer_id = 5;
```

**2. Use parameterized queries (prevent SQL injection and enable plan caching):**

```java
// BAD — SQL injection risk, no plan reuse
String sql = "SELECT * FROM users WHERE email = '" + email + "'";

// GOOD — parameterized
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);
```

**3. Pagination — keyset vs offset:**

```sql
-- OFFSET pagination (slow for large offsets — scans and discards rows)
SELECT id, name, created_at
FROM   products
ORDER BY created_at DESC
LIMIT  20 OFFSET 10000;  -- Must scan 10,020 rows!

-- KEYSET pagination (fast and stable — uses index directly)
SELECT id, name, created_at
FROM   products
WHERE  (created_at, id) < ('2024-06-15 10:30:00', 5432)
ORDER BY created_at DESC, id DESC
LIMIT  20;
```

**4. Use EXISTS instead of COUNT for existence checks:**

```sql
-- BAD: counts ALL matching rows when you only need to know if any exist
SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
FROM   orders WHERE customer_id = 5;

-- GOOD: stops at first match
SELECT EXISTS (SELECT 1 FROM orders WHERE customer_id = 5);
```

**5. Avoid functions on indexed columns in WHERE:**

```sql
-- BAD: prevents index use (applies function to every row)
SELECT * FROM users WHERE LOWER(email) = 'alice@example.com';

-- FIX 1: expression index
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

-- FIX 2: use citext type or collation
ALTER TABLE users ALTER COLUMN email TYPE citext;
```

**6. Batch operations instead of row-by-row:**

```sql
-- BAD: 1000 individual inserts
INSERT INTO audit_log(event, created_at) VALUES ('e1', now());
INSERT INTO audit_log(event, created_at) VALUES ('e2', now());
-- ... 998 more

-- GOOD: single multi-row insert
INSERT INTO audit_log(event, created_at)
VALUES ('e1', now()), ('e2', now()), /* ... */ ('e1000', now());

-- GOOD: COPY for bulk loading
COPY audit_log(event, created_at) FROM '/tmp/events.csv' CSV HEADER;
```

**7. Optimize JOINs — ensure join columns are indexed:**

```sql
-- Ensure foreign key columns have indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
-- PostgreSQL does NOT auto-create indexes on foreign keys (unlike MySQL)
```

**8. Materialized views for expensive aggregations:**

```sql
CREATE MATERIALIZED VIEW mv_daily_revenue AS
SELECT DATE_TRUNC('day', order_date) AS day,
       SUM(total) AS revenue,
       COUNT(*)   AS order_count
FROM   orders
GROUP BY DATE_TRUNC('day', order_date);

CREATE UNIQUE INDEX idx_mv_daily_revenue ON mv_daily_revenue(day);

-- Refresh (locks view; use CONCURRENTLY for zero-downtime refresh)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_revenue;
```

---

## 15. Transactions & Isolation Levels

---

### **Q77: What are the ACID properties?**

**ACID** is the set of guarantees that make database transactions reliable.

| Property | Definition | Concrete Example |
|---|---|---|
| **Atomicity** | A transaction is all-or-nothing. If any part fails, the entire transaction is rolled back. | Transferring $100 from Account A to Account B: debit A **and** credit B must both succeed or both fail. You never lose $100 into the void. |
| **Consistency** | A transaction transitions the database from one valid state to another. All constraints (NOT NULL, UNIQUE, FK, CHECK) are enforced. | An `INSERT` that violates a foreign key constraint is rejected entirely — the database never references a nonexistent parent row. |
| **Isolation** | Concurrent transactions do not interfere with each other (to a degree defined by the isolation level). | Two users simultaneously buying the last item in stock — isolation prevents both from succeeding (see Q78). |
| **Durability** | Once a transaction is committed, it survives system crashes, power failures, etc. | After `COMMIT`, the data is written to the WAL (Write-Ahead Log). Even if the server crashes immediately after, recovery replays the WAL. |

**PostgreSQL implementation:**

```sql
BEGIN;

-- Debit account A
UPDATE accounts SET balance = balance - 100.00 WHERE id = 1;

-- Check constraint (application-level)
DO $$
BEGIN
    IF (SELECT balance FROM accounts WHERE id = 1) < 0 THEN
        RAISE EXCEPTION 'Insufficient funds';
    END IF;
END $$;

-- Credit account B
UPDATE accounts SET balance = balance + 100.00 WHERE id = 2;

COMMIT;  -- Atomicity: all changes applied
         -- Durability: WAL flushed to disk
         -- If anything above throws, ROLLBACK undoes all changes
```

**Spring Boot transaction:**

```java
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;

    @Transactional // wraps method in BEGIN...COMMIT/ROLLBACK
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new AccountNotFoundException(fromId));
        Account to = accountRepository.findById(toId)
                .orElseThrow(() -> new AccountNotFoundException(toId));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromId, amount);
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        // On method exit: COMMIT if no exception, ROLLBACK if RuntimeException
    }
}
```

---

### **Q78: Explain the SQL isolation levels and their tradeoffs.**

Isolation levels control the visibility of uncommitted changes between concurrent transactions. Higher isolation = fewer anomalies but more contention.

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance | PostgreSQL Implementation |
|---|---|---|---|---|---|
| **Read Uncommitted** | Possible | Possible | Possible | Fastest | Maps to Read Committed in PG (PG never allows dirty reads) |
| **Read Committed** (PG default) | **No** | Possible | Possible | Good | Each statement sees a **new snapshot** of committed data |
| **Repeatable Read** | **No** | **No** | **No** (in PG) | Moderate | Entire transaction sees **one snapshot** from transaction start |
| **Serializable** | **No** | **No** | **No** | Slowest | Adds serialization conflict detection (SSI); may throw serialization errors |

**PostgreSQL specifics:**
- PG uses **MVCC** (Multi-Version Concurrency Control) — readers never block writers and vice versa.
- PG's Repeatable Read also prevents phantom reads (unlike the SQL standard minimum).
- Serializable in PG uses **Serializable Snapshot Isolation (SSI)** — optimistic, not lock-based. Transactions may be aborted with `ERROR: could not serialize access` and must be retried.

**Setting isolation in Spring @Transactional** (see [Section 11](#11-spring-boot-auto-configuration--starters)):

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public Report generateMonthlyReport(YearMonth month) {
    // All reads within this method see a consistent snapshot
    List<Order> orders = orderRepository.findByMonth(month);
    BigDecimal revenue = orders.stream()
            .map(Order::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new Report(month, revenue, orders.size());
}

@Transactional(isolation = Isolation.SERIALIZABLE)
public void claimCoupon(Long userId, Long couponId) {
    // Serializable: prevents two users from claiming 
    // the last coupon simultaneously
    Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow();
    if (coupon.getRemainingUses() <= 0) {
        throw new CouponExhaustedException(couponId);
    }
    coupon.setRemainingUses(coupon.getRemainingUses() - 1);
    claimRepository.save(new Claim(userId, couponId));
    // If concurrent transaction also claims, one gets a 
    // serialization error → retry
}
```

**Retry on serialization failure:**

```java
@Retryable(
    retryFor = CannotSerializeTransactionException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 50, multiplier = 2)
)
@Transactional(isolation = Isolation.SERIALIZABLE)
public void claimCoupon(Long userId, Long couponId) {
    // ... same as above
}
```

---

### **Q79: What are the concurrency anomalies in databases?**

| Anomaly | Description | Example | Prevented By |
|---|---|---|---|
| **Dirty Read** | Transaction reads data written by an uncommitted transaction that later rolls back. | T1 updates balance to $0; T2 reads $0; T1 rolls back → T2 acted on phantom data. | Read Committed and above |
| **Non-Repeatable Read** | Transaction reads the same row twice and gets different values because another transaction committed an UPDATE in between. | T1 reads balance=$100; T2 updates to $50 and commits; T1 reads again and gets $50. | Repeatable Read and above |
| **Phantom Read** | Transaction re-executes a query and gets a different set of rows because another transaction committed an INSERT/DELETE. | T1: `SELECT COUNT(*) FROM orders WHERE status='NEW'` → 5; T2 inserts a new order; T1 re-runs → 6. | Serializable (and PG Repeatable Read) |
| **Lost Update** | Two transactions read, then update the same row — the second update overwrites the first. | T1 and T2 both read stock=10; T1 sets stock=9; T2 sets stock=9 → one purchase "lost". | Pessimistic locking or Serializable |
| **Write Skew** | Two transactions read overlapping data, make disjoint writes, but the combined result violates a constraint. | Two doctors on-call; each checks "is there another doctor on call?" (yes), then removes themselves → nobody on call. | Serializable only |

**Demonstrating non-repeatable read in Read Committed:**

```sql
-- Session 1 (Read Committed)
BEGIN;
SELECT balance FROM accounts WHERE id = 1;  -- returns 100
-- ... wait ...
SELECT balance FROM accounts WHERE id = 1;  -- returns 50 (changed!)
COMMIT;

-- Session 2 (between Session 1's two SELECTs)
BEGIN;
UPDATE accounts SET balance = 50 WHERE id = 1;
COMMIT;
```

**Demonstrating phantom read:**

```sql
-- Session 1 (Read Committed)
BEGIN;
SELECT COUNT(*) FROM orders WHERE status = 'PENDING';  -- returns 5
-- ... wait ...
SELECT COUNT(*) FROM orders WHERE status = 'PENDING';  -- returns 6!
COMMIT;

-- Session 2 (between Session 1's two SELECTs)
BEGIN;
INSERT INTO orders(status) VALUES ('PENDING');
COMMIT;
```

---

### **Q80: Compare optimistic and pessimistic locking.**

| Aspect | Optimistic Locking | Pessimistic Locking |
|---|---|---|
| **Assumption** | Conflicts are **rare** | Conflicts are **frequent** |
| **Mechanism** | Version column checked at commit time | Database lock acquired at read time |
| **Lock duration** | No database locks held during processing | Lock held for entire transaction duration |
| **Conflict detection** | At UPDATE time — fails if version changed | At SELECT time — blocks other transactions |
| **Scalability** | High — no contention until commit | Lower — locks cause waiting and potential deadlocks |
| **Failure mode** | `OptimisticLockException` — retry at application layer | Blocked thread or timeout |
| **Best for** | Web apps, long user "think time", low contention | Short transactions, high contention, financial operations |

**Optimistic locking with JPA @Version:**

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer stockQuantity;

    @Version  // Hibernate auto-increments on each UPDATE
    private Long version;
    
    // getters, setters
}
```

Generated SQL on update:

```sql
UPDATE products 
SET    name = ?, stock_quantity = ?, version = 3
WHERE  id = 42 AND version = 2;
-- If 0 rows affected → version changed → OptimisticLockException
```

Handling the exception:

```java
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;

    @Retryable(
        retryFor = OptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void decrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow();
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(productId);
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
    }
}
```

**Pessimistic locking with SELECT FOR UPDATE:**

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
```

Generated SQL:

```sql
SELECT id, name, stock_quantity, version
FROM   products
WHERE  id = 42
FOR UPDATE;           -- Blocks other FOR UPDATE / UPDATE / DELETE on this row
-- FOR UPDATE NOWAIT;  -- Fails immediately if locked
-- FOR UPDATE SKIP LOCKED; -- Skips locked rows (great for job queues)
```

**SKIP LOCKED pattern for a job queue:**

```sql
-- Worker picks up the next unlocked job
BEGIN;
SELECT id, payload
FROM   job_queue
WHERE  status = 'PENDING'
ORDER BY created_at
LIMIT  1
FOR UPDATE SKIP LOCKED;

-- Process the job, then:
UPDATE job_queue SET status = 'DONE' WHERE id = ?;
COMMIT;
```

---

### **Q81: How do deadlocks occur in databases and how are they handled?**

A **deadlock** occurs when two (or more) transactions each hold a lock that the other needs, creating a cycle of dependencies where neither can proceed.

```
Transaction 1                          Transaction 2
─────────────                          ─────────────
BEGIN;                                 BEGIN;
UPDATE accounts SET ... WHERE id = 1;  UPDATE accounts SET ... WHERE id = 2;
-- holds lock on row 1                 -- holds lock on row 2

UPDATE accounts SET ... WHERE id = 2;  UPDATE accounts SET ... WHERE id = 1;
-- BLOCKED: waiting for row 2          -- BLOCKED: waiting for row 1
-- (held by T2)                        -- (held by T1)

                  ╔═══════════════╗
                  ║   DEADLOCK!   ║
                  ╚═══════════════╝
```

**PostgreSQL deadlock handling:**

1. **Detection:** PG runs a deadlock detector (default every `deadlock_timeout = 1s`). It builds a wait-for graph and checks for cycles.
2. **Resolution:** One transaction is chosen as the victim and rolled back with `ERROR: deadlock detected`.
3. **The other transaction** proceeds normally.

For a related discussion of deadlocks in Java (thread locks, `synchronized`, `ReentrantLock`), see [TopJavaConcurrencyInterviewQuestions.md](TopJavaConcurrencyInterviewQuestions.md).

**Prevention strategies:**

| Strategy | Description | Implementation |
|---|---|---|
| **Consistent lock ordering** | Always acquire locks in the same global order (e.g., by primary key ASC) | Sort IDs before locking: `WHERE id IN (1, 2) ORDER BY id FOR UPDATE` |
| **Lock timeout** | Fail fast instead of waiting indefinitely | `SET lock_timeout = '5s';` |
| **Reduce transaction scope** | Hold locks for the shortest possible time | Move non-DB work (API calls, file I/O) outside the transaction |
| **Use optimistic locking** | Avoid explicit locks entirely for low-contention cases | JPA `@Version` (see Q80) |
| **Advisory locks** | Application-level named locks that don't lock rows | `SELECT pg_advisory_lock(hashtext('transfer:1:2'))` |

**Lock ordering in Spring/JPA:**

```java
@Transactional
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    // Always lock in ascending ID order to prevent deadlocks
    Long firstId  = Math.min(fromId, toId);
    Long secondId = Math.max(fromId, toId);

    Account first  = accountRepository.findByIdForUpdate(firstId);
    Account second = accountRepository.findByIdForUpdate(secondId);

    Account from = fromId.equals(firstId) ? first : second;
    Account to   = toId.equals(firstId) ? first : second;

    from.debit(amount);
    to.credit(amount);
}
```

**Monitoring deadlocks in PostgreSQL:**

```sql
-- Check for current locks and blocking
SELECT blocked_locks.pid     AS blocked_pid,
       blocked_activity.usename  AS blocked_user,
       blocking_locks.pid    AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query    AS blocked_query,
       blocking_activity.query   AS blocking_query
FROM   pg_catalog.pg_locks blocked_locks
JOIN   pg_catalog.pg_stat_activity blocked_activity
         ON blocked_activity.pid = blocked_locks.pid
JOIN   pg_catalog.pg_locks blocking_locks
         ON blocking_locks.locktype = blocked_locks.locktype
        AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
        AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
        AND blocking_locks.pid != blocked_locks.pid
JOIN   pg_catalog.pg_stat_activity blocking_activity
         ON blocking_activity.pid = blocking_locks.pid
WHERE  NOT blocked_locks.granted;
```

---

## 16. Database Design & Normalization

---

### **Q82: Explain the normal forms (1NF through BCNF) with examples.**

**Normalization** eliminates data redundancy and prevents update anomalies by organizing columns and tables according to dependency rules.

| Normal Form | Rule | Violation Example | Fix |
|---|---|---|---|
| **1NF** | Every column contains **atomic** (single) values; no repeating groups or arrays. | `phone_numbers = '555-1234, 555-5678'` stored in one column | Create a separate `phone_numbers` table with one row per number |
| **2NF** | 1NF + every non-key column depends on the **entire** composite primary key (no partial dependencies). | PK = `(order_id, product_id)`, but `customer_name` depends only on `order_id` | Move `customer_name` to an `orders` table keyed by `order_id` |
| **3NF** | 2NF + no non-key column depends on **another non-key column** (no transitive dependencies). | `employee` table has `department_id`, `department_name` — `department_name` depends on `department_id`, not on the employee PK | Move `department_name` to a `departments` table |
| **BCNF** | 3NF + every **determinant** is a candidate key (handles edge cases where a non-candidate-key determines part of a candidate key). | Table `(student, course, professor)`: professor → course, but professor is not a candidate key | Split into `(student, professor)` and `(professor, course)` |

**1NF violation and fix:**

```sql
-- VIOLATION: repeating group in a column
CREATE TABLE orders_bad (
    order_id   INT PRIMARY KEY,
    customer   TEXT,
    products   TEXT   -- 'Widget,Gadget,Gizmo'  ← NOT atomic
);

-- FIX: separate table
CREATE TABLE orders (
    order_id   INT PRIMARY KEY,
    customer   TEXT
);
CREATE TABLE order_items (
    order_id   INT REFERENCES orders(order_id),
    product_id INT REFERENCES products(id),
    quantity   INT NOT NULL,
    PRIMARY KEY (order_id, product_id)
);
```

**2NF violation and fix:**

```sql
-- VIOLATION: composite PK, but customer_name depends only on order_id
CREATE TABLE order_details_bad (
    order_id      INT,
    product_id    INT,
    customer_name TEXT,       -- partial dependency on order_id only
    quantity      INT,
    PRIMARY KEY (order_id, product_id)
);

-- FIX: split into two tables
CREATE TABLE orders (
    order_id      INT PRIMARY KEY,
    customer_name TEXT
);
CREATE TABLE order_details (
    order_id   INT REFERENCES orders(order_id),
    product_id INT,
    quantity   INT,
    PRIMARY KEY (order_id, product_id)
);
```

**3NF violation and fix:**

```sql
-- VIOLATION: department_name transitively depends on employee_id
--            via department_id
CREATE TABLE employees_bad (
    employee_id     INT PRIMARY KEY,
    name            TEXT,
    department_id   INT,
    department_name TEXT   -- depends on department_id, not employee_id
);

-- FIX: separate departments table
CREATE TABLE departments (
    department_id   INT PRIMARY KEY,
    department_name TEXT
);
CREATE TABLE employees (
    employee_id   INT PRIMARY KEY,
    name          TEXT,
    department_id INT REFERENCES departments(department_id)
);
```

---

### **Q83: When should you denormalize?**

**Denormalization** intentionally introduces redundancy to improve **read performance** at the cost of write complexity and potential inconsistency.

**When to denormalize:**

| Scenario | Technique | Tradeoff |
|---|---|---|
| **Frequent expensive JOINs** | Store computed/duplicated columns on the parent table | Must update both places on writes |
| **Read-heavy dashboards** | Materialized views | Stale data between refreshes |
| **Event-sourced read models** | Projected read tables optimized per query | Eventual consistency |
| **Search/filtering** | Denormalized search index (Elasticsearch) | Sync lag |
| **Microservice boundary** | Duplicate reference data locally | Requires sync mechanism |
| **Audit/reporting** | Snapshot data at time of event (e.g., store product name + price on order_item, not just product_id) | Historical accuracy vs storage |

**Example — storing a computed count:**

```sql
-- Normalized: count reviews every time (expensive for product listing page)
SELECT p.*, (SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id) AS review_count
FROM products p;

-- Denormalized: maintain review_count on the products table
ALTER TABLE products ADD COLUMN review_count INT DEFAULT 0;

-- Keep it updated with a trigger
CREATE OR REPLACE FUNCTION update_review_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE products SET review_count = review_count + 1
        WHERE id = NEW.product_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE products SET review_count = review_count - 1
        WHERE id = OLD.product_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_review_count
AFTER INSERT OR DELETE ON reviews
FOR EACH ROW EXECUTE FUNCTION update_review_count();
```

**Materialized views for reporting:**

```sql
CREATE MATERIALIZED VIEW mv_product_stats AS
SELECT p.id,
       p.name,
       p.category,
       COUNT(r.id)        AS review_count,
       AVG(r.rating)      AS avg_rating,
       SUM(oi.quantity)    AS total_sold,
       SUM(oi.line_total)  AS total_revenue
FROM   products p
LEFT JOIN reviews r ON r.product_id = p.id
LEFT JOIN order_items oi ON oi.product_id = p.id
GROUP BY p.id, p.name, p.category;

-- Refresh periodically (e.g., every 5 minutes via pg_cron)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_product_stats;
```

**Rule of thumb:** Normalize first (3NF minimum). Denormalize only when you have **measured** performance data showing the JOINs are a bottleneck — and always document the denormalization and the mechanism that keeps redundant data in sync.

---

### **Q84: Compare primary key strategies — auto-increment vs UUID vs ULID.**

| Aspect | Auto-Increment (`BIGSERIAL`) | UUID v4 (`gen_random_uuid()`) | ULID / UUID v7 |
|---|---|---|---|
| **Size** | 8 bytes | 16 bytes | 16 bytes (ULID) / 16 bytes (UUID v7) |
| **Sortable** | Yes (monotonically increasing) | No (random) | Yes (timestamp prefix) |
| **Index performance** | Excellent — sequential inserts append to B-tree end | Poor — random inserts cause page splits and fragmentation | Good — timestamp prefix keeps inserts semi-sequential |
| **Guessability** | High — `id=1001` → next is `1002` | None (128-bit random) | Low (millisecond precision + random suffix) |
| **Distributed generation** | Requires coordination (sequences, id ranges) | No coordination needed | No coordination needed |
| **URL safe** | Yes (short) | Yes (36 chars with hyphens) | Yes (26 chars, Crockford Base32) |
| **Merge/replication** | Collision risk across databases | No collision risk | No collision risk |
| **Storage in PG** | `BIGINT` — 8 bytes, fast comparisons | `UUID` native type — 16 bytes | Store as `UUID` (v7) or `BYTEA`/`TEXT` |

**Implementation examples:**

```sql
-- Auto-increment (PostgreSQL)
CREATE TABLE orders (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    total NUMERIC(12, 2)
);

-- UUID v4 (PostgreSQL 13+)
CREATE TABLE orders (
    id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    total NUMERIC(12, 2)
);

-- UUID v7 (time-ordered; PG extension or application-generated)
-- Generate in Java:
```

```java
// UUID v7 generation in Java (requires Java 17+ and a library or manual impl)
// Using java.util.UUID with a time-ordered variant:
import com.github.f4b6a3.uuid.UuidCreator;

@Entity
public class Order {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id = UuidCreator.getTimeOrderedEpoch(); // UUID v7

    // Or with ULID:
    // private String id = UlidCreator.getMonotonicUlid().toString();
}
```

**Recommendation for most Spring Boot applications:**

- **Internal services, single database:** `BIGSERIAL` — simple, fast, compact.
- **APIs exposing IDs externally:** UUID v7 or ULID — non-guessable, no sequence info leak, time-sortable for good index performance.
- **Distributed systems / microservices:** UUID v7 — no coordination, merge-friendly, time-ordered for index locality.
- **Avoid UUID v4 as a clustered primary key** on large tables — random ordering causes severe B-tree page split churn and bloat.

---

### **Q85: Compare Flyway and Liquibase for schema migrations.**

Both are **version-controlled schema migration tools** that track which changes have been applied to a database.

| Aspect | Flyway | Liquibase |
|---|---|---|
| **Migration format** | SQL files (or Java classes) | XML, YAML, JSON, or SQL changesets |
| **Philosophy** | Simple, SQL-first | Abstraction layer (database-agnostic changesets) |
| **Naming convention** | `V1__Create_users.sql`, `V2__Add_email_index.sql` | Changelogs with changesets identified by author + id |
| **Rollback** | Manual (write `U1__Undo_create_users.sql`); auto-rollback in paid edition only | Built-in rollback support for many change types |
| **Diff / auto-generate** | Paid feature | Free — can diff two databases and generate changelog |
| **Multi-database support** | Via separate SQL files per dialect | Single changelog works across databases (uses abstractions) |
| **Spring Boot integration** | `spring.flyway.*` properties; auto-runs on startup | `spring.liquibase.*` properties; auto-runs on startup |
| **Learning curve** | Very low (just write SQL) | Moderate (learn changeset XML/YAML syntax) |
| **Tracking table** | `flyway_schema_history` | `databasechangelog` + `databasechangeloglock` |
| **Community** | Very popular for SQL-first teams | Popular in enterprises needing multi-DB support |
| **Conditional execution** | Limited (callbacks) | Preconditions, contexts, labels |

**Flyway example (Spring Boot):**

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_orders_table.sql
├── V3__add_email_unique_index.sql
└── V4__add_order_status_column.sql
```

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- V3__add_email_unique_index.sql
CREATE UNIQUE INDEX CONCURRENTLY idx_users_email ON users(email);
```

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

**Liquibase example:**

```yaml
# db/changelog/db.changelog-master.yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: dev
      changes:
        - createTable:
            tableName: users
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
              - column:
                  name: email
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
      rollback:
        - dropTable:
            tableName: users
```

**Recommendation:** Use **Flyway** if your team is comfortable writing raw SQL and targets a single database engine. Use **Liquibase** if you need rollback support, multi-database portability, or auto-generated diffs from an existing schema.

---

## 17. N+1 Problem & ORM Pitfalls

---

### **Q86: What is the N+1 query problem?**

The **N+1 problem** occurs when an ORM executes **1 query** to load a list of parent entities, then **N additional queries** — one per parent — to load each parent's associated children. This is typically caused by lazy loading.

**Example entity model:**

```java
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items;
}
```

**The N+1 in action:**

```java
// This looks innocent...
List<Order> orders = orderRepository.findAll();  // 1 query

for (Order order : orders) {
    // Each access triggers a lazy load → 1 query per order
    System.out.println(order.getItems().size());
}
```

**SQL log output (N=1000 orders):**

```sql
-- Query 1: fetch all orders
SELECT o.id, o.customer_id, o.order_date, o.total
FROM   orders o;

-- Query 2..1001: fetch items for EACH order
SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.line_total
FROM   order_items oi WHERE oi.order_id = 1;

SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.line_total
FROM   order_items oi WHERE oi.order_id = 2;

-- ... 998 more queries
```

**Total: 1 + 1000 = 1001 queries** instead of 1 or 2.

**How to detect:**
- Enable SQL logging: `spring.jpa.show-sql=true` or `logging.level.org.hibernate.SQL=DEBUG`
- Use **Hibernate Statistics**: `spring.jpa.properties.hibernate.generate_statistics=true`
- Use **datasource-proxy** or **P6Spy** for query counting in tests
- Use integration tests that assert maximum query counts:

```java
@Test
void shouldNotProduceNPlusOne() {
    // datasource-proxy query counter
    QueryCountHolder.clear();
    
    List<OrderSummaryDto> result = orderService.getOrderSummaries();
    
    assertThat(result).hasSize(50);
    assertThat(QueryCountHolder.getGrandTotal().getSelect()).isLessThanOrEqualTo(2);
}
```

For more on Spring Data JPA repository configuration, see (see [Section 10](#10-spring-data-jpa--persistence)).

---

### **Q87: What are the solutions to the N+1 problem?**

| Solution | Approach | Pros | Cons |
|---|---|---|---|
| `JOIN FETCH` (JPQL) | Eager-load association in a single query with JOIN | Simple, one query | Cartesian product with multiple collections; pagination breaks |
| `@EntityGraph` | Declarative fetch plan on repository method | Clean, no JPQL needed | Same Cartesian product issues |
| `@BatchSize` | Loads associations in batches (e.g., 25 at a time) | Works with pagination; no Cartesian product | Still multiple queries (N/batch) |
| `@Fetch(SUBSELECT)` | Loads all associations in one subselect | Two queries total | Global setting on entity; cannot customize per query |
| **DTO projection** | Select only needed columns, no entity mapping | Best performance; no lazy load risk | More code; no managed entities |

**JOIN FETCH:**

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.items
        JOIN FETCH o.customer
        WHERE o.orderDate >= :since
        """)
    List<Order> findOrdersWithDetails(@Param("since") LocalDate since);
}
```

Generated SQL:

```sql
SELECT DISTINCT o.*, oi.*, c.*
FROM   orders o
JOIN   order_items oi ON oi.order_id = o.id
JOIN   customers c ON c.id = o.customer_id
WHERE  o.order_date >= ?;
-- Single query, all data loaded
```

**@EntityGraph:**

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "customer"})
    List<Order> findByOrderDateAfter(LocalDate since);
}
```

**@BatchSize (on the entity collection):**

```java
@Entity
public class Order {

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @BatchSize(size = 25)  // Load items in batches of 25 orders
    private List<OrderItem> items;
}
```

Generated SQL:

```sql
-- Query 1: fetch orders
SELECT * FROM orders;

-- Query 2: batch-load items for first 25 orders
SELECT * FROM order_items WHERE order_id IN (1,2,3,...,25);

-- Query 3: batch-load items for next 25 orders
SELECT * FROM order_items WHERE order_id IN (26,27,...,50);
```

**DTO projection (best for read-only use cases):**

```java
public record OrderSummaryDto(
    Long orderId,
    String customerName,
    LocalDate orderDate,
    BigDecimal total,
    int itemCount
) {}

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT new com.example.dto.OrderSummaryDto(
            o.id, c.name, o.orderDate, o.total,
            (SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order = o)
        )
        FROM Order o
        JOIN o.customer c
        WHERE o.orderDate >= :since
        ORDER BY o.orderDate DESC
        """)
    List<OrderSummaryDto> findOrderSummaries(@Param("since") LocalDate since);
}
```

**Warning about JOIN FETCH + pagination:**

```java
// THIS BREAKS: Hibernate fetches ALL rows into memory, then paginates in-memory
@Query("SELECT o FROM Order o JOIN FETCH o.items")
Page<Order> findAll(Pageable pageable); // HHH90003004 warning

// FIX: Use two queries — one for IDs (paginated), one for full data
@Query("SELECT o.id FROM Order o WHERE o.orderDate >= :since")
Page<Long> findOrderIds(@Param("since") LocalDate since, Pageable pageable);

@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.id IN :ids")
List<Order> findOrdersWithItemsByIds(@Param("ids") List<Long> ids);
```

For related query optimization techniques, see (see [Section 14](#14-indexes--query-optimization)).

---

### **Q88: What is the Open Session in View anti-pattern?**

**Open Session in View (OSIV)** is a pattern where the Hibernate `Session` (and its associated database connection) remains open for the entire HTTP request lifecycle — including during **view rendering** (template processing, JSON serialization).

**Spring Boot enables OSIV by default** (`spring.jpa.open-in-view=true`) and logs a warning at startup:

```
WARN: spring.jpa.open-in-view is enabled by default. Therefore, database queries 
may be performed during view rendering. Explicitly configure spring.jpa.open-in-view 
to disable this warning.
```

**How it works:**

```
HTTP Request Lifecycle with OSIV=true:
┌─────────────────────────────────────────────────────────┐
│ OpenEntityManagerInViewInterceptor                       │
│                                                          │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────────┐ │
│  │Controller │ → │Service       │ → │JSON Serialization│ │
│  │           │   │@Transactional│   │(view rendering)  │ │
│  └──────────┘   └──────────────┘   └──────────────────┘ │
│                                                          │
│  ←── EntityManager (and DB connection) open the ────→    │
│       entire time, including serialization                │
└─────────────────────────────────────────────────────────┘
```

**Why it is considered harmful:**

| Problem | Explanation |
|---|---|
| **DB connection held too long** | Connection is occupied during JSON serialization, template rendering, or slow network I/O. With a pool of 10 connections, 10 slow requests can exhaust the pool. |
| **Hidden N+1 queries** | Lazy-loaded associations are transparently fetched during serialization — developers don't notice the extra queries because there is no `LazyInitializationException`. |
| **Unpredictable performance** | Adding a new field to a JSON response can trigger unexpected database queries. |
| **No transaction during view** | Queries during rendering execute outside the `@Transactional` boundary — they run in auto-commit mode, potentially seeing inconsistent data. |
| **Harder to test** | Service layer tests pass, but production has N+1 because serialization is where the lazy loads happen. |

**Recommended: disable OSIV:**

```yaml
spring:
  jpa:
    open-in-view: false
```

After disabling, you will get `LazyInitializationException` if you access an unloaded association outside a transaction. This is **a good thing** — it forces you to explicitly decide what data to load:

- Use `JOIN FETCH` or `@EntityGraph` for needed associations (see Q87).
- Use DTO projections to return exactly the data the API needs.
- Initialize collections within the `@Transactional` service method.

---

### **Q89: When should you skip the ORM and use native queries or alternatives?**

JPA/Hibernate is excellent for CRUD-heavy domain-driven applications, but it adds overhead and complexity that is counterproductive for certain workloads.

**When to skip the ORM:**

| Scenario | Why ORM Hurts | Alternative |
|---|---|---|
| **Bulk inserts/updates** (thousands+ rows) | Hibernate flushes one statement at a time; dirty checking overhead | `JdbcTemplate.batchUpdate()`, `COPY`, or `spring-batch` |
| **Complex analytical queries** | JPQL lacks window functions, CTEs, lateral joins | Native SQL via `@NativeQuery` or jOOQ |
| **Performance-critical hot paths** | Entity hydration, proxy creation, dirty checking add latency | DTO projection or jOOQ |
| **Schema you don't control** | Legacy tables with no clear entity mapping | `JdbcTemplate` or jOOQ |
| **Dynamic query construction** | Criteria API is verbose and hard to debug | jOOQ or Querydsl |

**Native query in Spring Data JPA:**

```java
@Query(value = """
    WITH monthly AS (
        SELECT DATE_TRUNC('month', order_date) AS month,
               customer_id,
               SUM(total) AS monthly_spend
        FROM   orders
        WHERE  order_date >= :since
        GROUP BY 1, 2
    )
    SELECT customer_id, month, monthly_spend,
           SUM(monthly_spend) OVER (
               PARTITION BY customer_id ORDER BY month
           ) AS cumulative_spend
    FROM monthly
    ORDER BY customer_id, month
    """, nativeQuery = true)
List<Object[]> findCumulativeSpend(@Param("since") LocalDate since);
```

**JdbcTemplate for batch inserts:**

```java
@Repository
@RequiredArgsConstructor
public class EventBatchRepository {

    private final JdbcTemplate jdbc;

    public void batchInsert(List<Event> events) {
        jdbc.batchUpdate(
            "INSERT INTO events(type, payload, created_at) VALUES (?, ?::jsonb, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Event e = events.get(i);
                    ps.setString(1, e.type());
                    ps.setString(2, e.payload());
                    ps.setTimestamp(3, Timestamp.valueOf(e.createdAt()));
                }
                @Override
                public int getBatchSize() { return events.size(); }
            }
        );
    }
}
```

**jOOQ — type-safe SQL with full SQL power:**

```java
@Repository
@RequiredArgsConstructor
public class OrderAnalyticsRepository {

    private final DSLContext dsl;

    public List<TopCustomerDto> findTopCustomers(LocalDate since) {
        return dsl
            .select(
                CUSTOMERS.NAME,
                count(ORDERS.ID).as("order_count"),
                sum(ORDERS.TOTAL).as("total_spend"),
                rank().over(orderBy(sum(ORDERS.TOTAL).desc())).as("spend_rank")
            )
            .from(ORDERS)
            .join(CUSTOMERS).on(CUSTOMERS.ID.eq(ORDERS.CUSTOMER_ID))
            .where(ORDERS.ORDER_DATE.ge(since))
            .groupBy(CUSTOMERS.NAME)
            .orderBy(field("total_spend").desc())
            .limit(10)
            .fetchInto(TopCustomerDto.class);
    }
}
```

**Decision framework:** Use JPA for domain entities with lifecycle management (create, update, delete, relationships). Use native SQL, JdbcTemplate, or jOOQ for reporting, analytics, bulk operations, and anything where you need full SQL expressiveness (see [Section 10](#10-spring-data-jpa--persistence) for Spring Data JPA fundamentals).

---

## 18. Scaling, Caching & NoSQL

---

### **Q90: How does connection pooling work, and how do you configure HikariCP?**

A **connection pool** maintains a set of reusable database connections. Creating a new JDBC connection involves TCP handshake, TLS negotiation, and authentication — typically 50-200ms. A pool amortizes this cost across requests.

**HikariCP** is the default connection pool in Spring Boot 3.x — it is the fastest Java connection pool.

**How it works:**

```
Application Threads                    HikariCP Pool                  PostgreSQL
┌──────────┐                     ┌────────────────────┐
│ Thread 1 │── getConnection() →│ ┌──┐ ┌──┐ ┌──┐ ┌──┐│        ┌──────────┐
│ Thread 2 │── getConnection() →│ │C1│ │C2│ │C3│ │C4││← TCP →│  DB      │
│ Thread 3 │── waiting...       │ └──┘ └──┘ └──┘ └──┘│        │  Server  │
│ Thread 4 │── getConnection() →│  (max pool size=4)  │        └──────────┘
└──────────┘                     └────────────────────┘
                                  Thread 3 waits until a
                                  connection is returned
```

**Sizing formula (from the HikariCP wiki):**

```
pool_size = (core_count * 2) + effective_spindle_count

Example: 4-core server with SSD (spindle_count ≈ 1)
pool_size = (4 * 2) + 1 = 9
```

For most web applications, **10-20 connections** is sufficient. More connections means more context switching at the database, which can **decrease** throughput. PostgreSQL with 100+ connections per application instance usually indicates a problem.

**Spring Boot configuration:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      # Pool sizing
      maximum-pool-size: 10          # Max connections in pool
      minimum-idle: 5                # Min idle connections to maintain
      
      # Timeouts
      connection-timeout: 30000      # Max wait for connection (ms), fail-fast
      idle-timeout: 600000           # Max idle time before eviction (ms, 10 min)
      max-lifetime: 1800000          # Max connection lifetime (ms, 30 min)
                                     # Set < DB server timeout (e.g., PG wait_timeout)
      
      # Validation
      keepalive-time: 300000         # Send keepalive every 5 min to prevent firewall drops
      
      # Leak detection
      leak-detection-threshold: 60000  # Log warning if connection not returned within 60s
      
      # Metrics
      pool-name: HikariPool-Primary
      register-mbeans: true           # JMX monitoring
```

**Monitoring HikariCP with Micrometer (Spring Boot Actuator):**

```yaml
management:
  metrics:
    enable:
      hikaricp: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

Key metrics to monitor:

| Metric | Meaning | Alert Threshold |
|---|---|---|
| `hikaricp_connections_active` | Currently borrowed connections | Approaching max pool size |
| `hikaricp_connections_pending` | Threads waiting for a connection | > 0 for extended period |
| `hikaricp_connections_timeout_total` | Timeout exceptions | Any non-zero value |
| `hikaricp_connections_usage_seconds` | How long connections are held | P99 > 1s indicates slow queries or missing `@Transactional` scope |

**Common mistakes:**
- Setting `maximum-pool-size` to 50+ "just in case" — this actually hurts PostgreSQL performance.
- Not setting `max-lifetime` — connections can become stale if the DB restarts.
- Holding connections during external API calls inside `@Transactional` — consider moving HTTP calls outside the transaction.

---

### **Q91: Explain caching strategies and Spring's @Cacheable.**

| Strategy | Description | Consistency | Use Case |
|---|---|---|---|
| **Cache-Aside** (Lazy Loading) | Application checks cache first; on miss, reads from DB and populates cache | Application manages; stale until TTL or eviction | General purpose; most common |
| **Read-Through** | Cache itself loads from DB on miss (transparent to application) | Cache handles loading | Libraries like Caffeine's `LoadingCache` |
| **Write-Through** | Writes go to cache AND DB synchronously | Strong consistency | When reads must always see latest writes |
| **Write-Behind** (Write-Back) | Writes go to cache; DB updated asynchronously | Eventual consistency; risk of data loss | High write throughput (e.g., analytics counters) |
| **Refresh-Ahead** | Cache proactively refreshes entries before expiry | Reduces cache miss latency | Frequently accessed keys with predictable patterns |

**Cache-Aside flow:**

```
  ┌────────┐    1. GET key    ┌───────┐
  │  App   │ ──────────────→  │ Cache │
  │        │ ←────────────── │       │  2a. HIT → return cached value
  │        │   cache miss     └───────┘
  │        │                      
  │        │  2b. MISS: query DB          
  │        │ ──────────────→  ┌────┐
  │        │ ←────────────── │ DB │  3. Return result
  │        │                  └────┘
  │        │  4. PUT into cache
  │        │ ──────────────→  ┌───────┐
  └────────┘                  │ Cache │
                              └───────┘
```

**Spring Boot @Cacheable with Caffeine (local cache):**

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return manager;
    }
}

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    public ProductDto getProduct(Long id) {
        // Only called on cache miss
        return productRepository.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CachePut(value = "products", key = "#result.id")
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setName(request.name());
        product.setPrice(request.price());
        return ProductDto.from(productRepository.save(product));
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Scheduled(fixedRate = 3600000) // Every hour
    public void evictAllProducts() {
        // Scheduled full cache clear
    }
}
```

**Spring Boot with Redis (distributed cache):**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 600000      # 10 minutes
      cache-null-values: false   # Don't cache null results
      key-prefix: "myapp:"
      use-key-prefix: true
```

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                    SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withCacheConfiguration("products",
                    config.entryTtl(Duration.ofMinutes(30)))  // Per-cache TTL
                .withCacheConfiguration("user-sessions",
                    config.entryTtl(Duration.ofHours(2)))
                .build();
    }
}
```

**Cache invalidation pitfalls:**
- **Stale data:** Always set a TTL. "There are only two hard things in computer science: cache invalidation and naming things."
- **Thundering herd:** When a popular cache entry expires, many threads simultaneously query the DB. Mitigate with lock-based loading (`Caffeine.newBuilder().refreshAfterWrite()`).
- **Inconsistency on writes:** `@CachePut` updates the cache, but another instance's cache is stale. Use Redis for shared cache across instances.

---

### **Q92: When should you use SQL vs NoSQL databases?**

**CAP Theorem:** A distributed data store can provide at most **two of three** guarantees simultaneously:
- **C**onsistency — every read returns the most recent write
- **A**vailability — every request receives a response (no errors)
- **P**artition tolerance — system continues operating despite network partitions

In practice, network partitions **always happen** in distributed systems, so the real choice is **CP** (consistent but may be unavailable) vs **AP** (available but may return stale data).

| Aspect | SQL (Relational) | Document (MongoDB) | Key-Value (Redis) | Wide-Column (Cassandra) | Graph (Neo4j) |
|---|---|---|---|---|---|
| **Data model** | Tables, rows, relationships | JSON-like documents | Simple key → value | Column families, wide rows | Nodes, edges, properties |
| **Schema** | Strict (enforced) | Flexible (schema-on-read) | None | Flexible per row | Flexible |
| **Query language** | SQL | MQL (MongoDB Query Language) | GET/SET commands | CQL (Cassandra Query Language) | Cypher |
| **Transactions** | Full ACID | Multi-document ACID (4.0+) | Single-key atomic; Lua scripts | Lightweight (per-partition) | ACID |
| **Scaling** | Vertical + read replicas | Horizontal (sharding) | Horizontal (cluster) | Horizontal (built-in) | Vertical primarily |
| **CAP trade-off** | CP (single node) | CP or AP (configurable) | AP (cluster) or CP (single) | AP | CP |
| **Best for** | Complex queries, joins, integrity | Content management, catalogs, user profiles | Caching, sessions, leaderboards | Time-series, IoT, logging | Social networks, recommendations, fraud detection |
| **Avoid when** | Schema changes constantly; massive horizontal scale needed | Complex multi-entity transactions, heavy joins | Complex queries, large values (> 512MB) | Need strong consistency, ad-hoc queries | Simple CRUD, tabular data |

**Decision flowchart (simplified):**

```
Do you need complex joins and referential integrity?
├── YES → SQL (PostgreSQL, MySQL)
└── NO
    ├── Is the data a natural document / nested structure?
    │   └── YES → Document DB (MongoDB, DynamoDB)
    ├── Do you need sub-millisecond access by key?
    │   └── YES → Key-Value (Redis, Memcached)
    ├── Is it write-heavy time-series / append-only?
    │   └── YES → Wide-Column (Cassandra, ScyllaDB) or TimescaleDB
    └── Is the core problem about relationships and graph traversal?
        └── YES → Graph DB (Neo4j)
```

**PostgreSQL as a "multi-model" database:**

```sql
-- JSON document storage
CREATE TABLE events (
    id   BIGINT GENERATED ALWAYS AS IDENTITY,
    data JSONB NOT NULL
);
CREATE INDEX idx_events_data ON events USING gin(data);
SELECT * FROM events WHERE data @> '{"type": "purchase", "amount": 99.99}';

-- Key-value via hstore
CREATE EXTENSION hstore;
SELECT 'name => "Alice", role => "admin"'::hstore -> 'role';

-- Full-text search (avoids Elasticsearch for simple cases)
ALTER TABLE products ADD COLUMN search_vector tsvector;
CREATE INDEX idx_products_search ON products USING gin(search_vector);
SELECT * FROM products WHERE search_vector @@ to_tsquery('wireless & headphones');
```

---

### **Q93: How do read replicas and sharding enable horizontal scaling?**

**Read replicas (scale reads):**

```
                    Writes
  ┌──────────┐  ──────────→  ┌────────────┐
  │ App      │               │  Primary   │
  │ (Spring) │               │  (Leader)  │
  │          │  ←── reads ── │            │
  └──────────┘               └─────┬──────┘
       │                           │ Replication (async)
       │                     ┌─────┴──────┐
       │    ←── reads ───── │  Replica 1  │
       │                     └────────────┘
       │                     ┌────────────┐
       └───── reads ───────→ │  Replica 2  │
                             └────────────┘
```

**Spring Boot routing to read replicas:**

```java
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? DataSourceType.REPLICA
                : DataSourceType.PRIMARY;
    }
}

// Usage: read-only transactions automatically route to replicas
@Transactional(readOnly = true)
public List<ProductDto> searchProducts(String query) {
    return productRepository.search(query);  // goes to replica
}

@Transactional
public Product createProduct(CreateProductRequest req) {
    return productRepository.save(new Product(req));  // goes to primary
}
```

**Replication lag concern:** Replicas may be milliseconds to seconds behind the primary. After a write, an immediate read from a replica may not see the change. Solutions:
- Read from primary for "read-your-own-writes" consistency.
- Use session affinity to pin a user to a replica.
- Check replication lag (`pg_stat_replication`) and route accordingly.

**Sharding (scale writes and data volume):**

```
  ┌──────────┐
  │  App     │
  │ (Router) │
  └────┬─────┘
       │  shard_key = customer_id
       │
  ┌────┴────────────────────────────┐
  │         Shard Router            │
  │  hash(customer_id) % N_shards  │
  └────┬──────────┬──────────┬──────┘
       │          │          │
  ┌────┴───┐ ┌───┴────┐ ┌───┴────┐
  │ Shard 0│ │ Shard 1│ │ Shard 2│
  │ 0..33% │ │34..66% │ │67..100%│
  └────────┘ └────────┘ └────────┘
```

**Shard key selection criteria:**

| Criterion | Good | Bad |
|---|---|---|
| **Even distribution** | `customer_id` (many values) | `country` (skewed — US gets most traffic) |
| **Query locality** | Key used in most WHERE clauses | Key rarely queried (forces cross-shard queries) |
| **Growth** | Monotonic keys with consistent hashing | Fixed ranges that require manual rebalancing |
| **Joins** | Co-locate related data on same shard | Frequent cross-shard joins (expensive) |

**Consistent hashing** maps keys to a ring of virtual nodes, so adding/removing a shard only moves ~1/N of the keys (vs rehashing everything).

**Sharding approaches:**

| Approach | Description | Example |
|---|---|---|
| **Application-level** | App decides which shard to query | Multi-tenant SaaS with tenant_id routing |
| **Proxy-level** | Proxy (e.g., Citus, Vitess, ProxySQL) handles routing | Transparent to application |
| **Database-native** | Built into the database (Citus for PostgreSQL, MongoDB sharding) | Declarative shard key configuration |

---

### **Q94: How does the database-per-service pattern work in microservices?**

In a **microservice architecture**, each service owns its own database. No service directly accesses another service's database. This provides:
- **Loose coupling:** Services can evolve schemas independently.
- **Technology freedom:** Order service uses PostgreSQL; Analytics service uses ClickHouse.
- **Independent scaling:** Each database scales based on its service's load.

```
┌────────────┐    ┌─────────────┐    ┌──────────────┐
│ Order      │    │ Inventory   │    │ Payment      │
│ Service    │    │ Service     │    │ Service      │
└─────┬──────┘    └──────┬──────┘    └──────┬───────┘
      │                  │                   │
  ┌───┴───┐         ┌───┴───┐          ┌───┴───┐
  │Order  │         │Invent.│          │Payment│
  │  DB   │         │  DB   │          │  DB   │
  └───────┘         └───────┘          └───────┘
   (PostgreSQL)     (PostgreSQL)       (PostgreSQL)
```

**Challenge: distributed transactions.** You cannot use a single `BEGIN/COMMIT` across multiple databases. Two patterns address this:

**1. Saga Pattern (choreography or orchestration):**

A **saga** is a sequence of local transactions. Each service executes its transaction and publishes an event. If a step fails, **compensating transactions** undo the previous steps.

**Choreography (event-driven, no central coordinator):**

```
Order Service         Inventory Service       Payment Service
     │                       │                       │
     │  OrderCreated event   │                       │
     │──────────────────────→│                       │
     │                       │ reserve stock         │
     │                       │ StockReserved event   │
     │                       │──────────────────────→│
     │                       │                       │ charge card
     │                       │                       │ PaymentCompleted
     │←─────────────────────────────────────────────│
     │ confirm order                                 │
     
 On failure (e.g., payment fails):
     │                       │  PaymentFailed event  │
     │                       │←──────────────────────│
     │                       │ release stock (compensate)
     │  StockReleased event  │                       │
     │←──────────────────────│                       │
     │ cancel order (compensate)                     │
```

**Orchestration (central saga coordinator):**

```java
@Component
@RequiredArgsConstructor
public class CreateOrderSaga {

    private final OrderService orderService;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    @Transactional
    public OrderResult execute(CreateOrderCommand cmd) {
        // Step 1: Create order in PENDING state
        Order order = orderService.createPendingOrder(cmd);

        try {
            // Step 2: Reserve inventory
            inventoryClient.reserveStock(order.getItems());

            // Step 3: Process payment
            paymentClient.charge(order.getCustomerId(), order.getTotal());

            // All steps succeeded
            order.setStatus(OrderStatus.CONFIRMED);
            return OrderResult.success(order);

        } catch (InsufficientStockException e) {
            // Compensate: cancel order
            order.setStatus(OrderStatus.CANCELLED);
            return OrderResult.failure("Insufficient stock");

        } catch (PaymentFailedException e) {
            // Compensate: release inventory, cancel order
            inventoryClient.releaseStock(order.getItems());
            order.setStatus(OrderStatus.CANCELLED);
            return OrderResult.failure("Payment failed");
        }
    }
}
```

**2. Outbox Pattern (reliable event publishing):**

Ensures events are published **atomically** with the local database transaction (avoids dual-write problem).

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    @Transactional  // Single local transaction
    public Order createOrder(CreateOrderCommand cmd) {
        Order order = new Order(cmd);
        orderRepository.save(order);

        // Write event to outbox table (same DB, same transaction)
        outboxRepository.save(new OutboxEvent(
            "OrderCreated",
            order.getId().toString(),
            objectMapper.writeValueAsString(new OrderCreatedEvent(order))
        ));

        return order;
        // A separate poller/CDC reads the outbox table and publishes to Kafka
    }
}
```

```sql
-- Outbox table
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at)
WHERE published = FALSE;
```

**Eventual consistency:** In the database-per-service model, the system is **eventually consistent**. This is acceptable for most business operations — an order might briefly show as "processing" before inventory and payment confirmations arrive. Design UIs and APIs to communicate this (e.g., return `202 Accepted` instead of `200 OK` for asynchronous operations).

| Pattern | Consistency | Complexity | Best For |
|---|---|---|---|
| **Saga (choreography)** | Eventual | Medium (event design) | Loosely coupled services with simple flows |
| **Saga (orchestration)** | Eventual | Medium-High (coordinator logic) | Complex multi-step workflows |
| **Outbox + CDC** | Eventual (reliable delivery) | Medium (infra: Debezium/Kafka) | Reliable event publishing without dual-write |
| **2PC (distributed transaction)** | Strong | High (coordinator + blocking) | Rarely used in microservices — brittle, slow |

---

## Quick-Reference Decision Matrix

| Problem | Tool / Pattern |
|---------|---------------|
| Object with many optional fields | **Builder** pattern |
| Swappable algorithms at runtime | **Strategy** pattern |
| Class hierarchy with fixed set of subtypes | **Sealed interfaces** (Java 17+) |
| Thread-safe key-value store | `ConcurrentHashMap` |
| Ordered unique elements | `TreeSet` / `TreeMap` |
| Efficient bulk inserts | `JdbcTemplate.batchUpdate()` or COPY |
| Type-safe configuration | `@ConfigurationProperties` with records |
| Controller exception handling | `@ControllerAdvice` + `ProblemDetail` |
| N+1 query resolution | `JOIN FETCH`, `@EntityGraph`, DTO projection |
| Optimistic concurrency control | JPA `@Version` column |
| Cross-service data consistency | **Saga pattern** + **Outbox** |
| Read-heavy scaling | Read replicas + `readOnly = true` routing |
| Low-latency lookups by key | Redis / Caffeine cache (`@Cacheable`) |
| Complex analytical SQL | Native queries, jOOQ, or CTEs |
| Schema versioning | **Flyway** (SQL migrations) or **Liquibase** (XML/YAML) |

---

## Cross-Part Connections

| Topic A | ↔ | Topic B | Connection |
|---------|---|---------|------------|
| `@Transactional` propagation (Sec 11) | ↔ | DB isolation levels (Sec 15) | Spring isolation maps directly to SQL standard levels |
| N+1 problem (Sec 17) | ↔ | Spring Data JPA (Sec 10) + Indexes (Sec 14) | `JOIN FETCH` / `@EntityGraph` avoid N+1; proper indexes make the join fast |
| Collections internals (Sec 2) | ↔ | JPA entities | `equals()`/`hashCode()` contract must be correct for entities in `Set` or `Map` |
| Memory leaks (Sec 6) | ↔ | Connection pool leaks (Sec 18) | Unclosed connections leak both heap memory and pool slots |
| Design patterns (Sec 1) | ↔ | Spring stereotype annotations (Sec 7) | Strategy ↔ `@Service` with interface; Factory ↔ `@Configuration` + `@Bean` |
| Generics & type system (Sec 3) | ↔ | Spring DI | Spring uses generics for type-safe injection (`List<Validator<Order>>`) |
| Exception handling (Sec 4) | ↔ | `@ControllerAdvice` (Sec 9) | Java exception hierarchy ↔ Spring global exception mapping |
| Streams & collectors (Sec 5) | ↔ | DTO projections (Sec 17) | Stream pipelines transform JPA results into DTOs efficiently |
| GC algorithms (Sec 6) | ↔ | Connection pool sizing (Sec 18) | GC pauses affect connection checkout latency; ZGC helps |
| `@Transactional(readOnly)` (Sec 11) | ↔ | Read replicas (Sec 18) | `readOnly = true` can route to replicas via `AbstractRoutingDataSource` |

---

## Common Anti-Patterns Across All Three Areas

### Java Core
1. **Using `==` to compare strings** — always use `.equals()`.
2. **Mutable state in streams** — streams should be side-effect free.
3. **Catching `Exception` / `Throwable` broadly** — catch the most specific type.
4. **Not overriding both `equals()` and `hashCode()`** — breaks HashMap/HashSet behavior.
5. **String concatenation in loops** — use `StringBuilder` or `Collectors.joining()`.

### Spring Boot
6. **`@Transactional` on private methods** — CGLIB proxies cannot intercept them.
7. **Self-invocation bypassing proxy** — extract to a separate bean.
8. **`@SpringBootTest` for everything** — use test slices for focused, faster tests.
9. **Holding DB connections during external API calls** — move HTTP calls outside `@Transactional`.
10. **Ignoring checked exception rollback** — use `rollbackFor = Exception.class`.

### Databases
11. **N+1 queries** — always profile SQL logs; use `JOIN FETCH` or `@EntityGraph`.
12. **`SELECT *` in production** — select only needed columns.
13. **Over-indexing** — each index slows writes; analyze before adding.
14. **Offset-based pagination on large tables** — use keyset pagination.
15. **OSIV (Open Session in View)** — disable it and handle lazy loading explicitly.

---

## Recommended Reading

- **Effective Java (3rd Edition)** by Joshua Bloch — essential Java best practices.
- **Spring in Action (6th Edition)** by Craig Walls — comprehensive Spring Boot guide.
- **Designing Data-Intensive Applications** by Martin Kleppmann — distributed systems and databases.
- **Java Concurrency in Practice** by Brian Goetz — the definitive Java concurrency book.
- **High-Performance Java Persistence** by Vlad Mihalcea — JPA/Hibernate optimization.
- **SQL Performance Explained** by Markus Winand — indexing and query optimization.

---

## Related Resources in This Repo

- **[TopJavaConcurrencyInterviewQuestions.md](TopJavaConcurrencyInterviewQuestions.md)** — 73 Q&A on Java concurrency (JMM, threads, executors, locks, atomics, virtual threads).
- **[TransactionValidator README](src/main/java/howToSolveCodingProblems/transactionValidator/README.md)** — Working example of design patterns (Result Pattern with sealed interfaces), AtomicReference, ReentrantLock, ConcurrentHashMap.
- **[Account.java](src/main/java/howToSolveCodingProblems/transactionValidator/model/Account.java)** — Thread-safe account with three concurrency mechanisms.
- **[ValidationResult.java](src/main/java/howToSolveCodingProblems/transactionValidator/validation/ValidationResult.java)** — Sealed interface implementing the Result Pattern (see Q6, Design Patterns).
- **[JavaInterviewTips.md](JavaInterviewTips.md)** — General Java interview tips and resources.
