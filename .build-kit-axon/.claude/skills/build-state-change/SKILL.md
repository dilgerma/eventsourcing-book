---
name: build-state-change
authors:
  - Mateusz Nowak
  - Martin Dilger
description: >
  Implement Event Sourcing write slices using Axon Framework 5, Vertical Slice Architecture, and
  Event Modeling patterns. A write slice is: Command → decide → Events → evolve → State.
  Use when: (1) implementing a new write slice / command handler in an AF5 Java project,
  (2) migrating/porting a write slice from Axon Framework 4 (Java or Kotlin) to AF5,
  (3) user provides a specification, Event Modeling artifact, existing tests, or natural language
  description of a command and asks to implement it,
  (4) user says "implement", "create", "add", "migrate", "port" a write slice, command handler,
  or aggregate behavior in an Axon Framework 5 / Vertical Slice Architecture project.
---

# Axon Framework 5 — Write Slice (Java)

## Step 0: Discover Target Project Conventions

> **Comments & description**: Each element in the slice carries a `comments: string[]` array and a `description` field. Use these as implementation hints. When done, resolve each used comment: `POST <BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/<nodeId>/comments/<commentId>/resolve` (get IDs first via GET on same path).

Before writing any code, read the target project's `CLAUDE.md` and explore at least one existing write
slice under `de.eventmodelers.slices`. Look for:

- Package layout within a slice (`write/{slicename}/` vs `{slicename}/write/`)
- Visibility conventions (`public`, package-private)
- Event hierarchy (sealed interface per context, tag property pattern)
- Metadata keys used on commands and events
- Feature flag approach (`@ConditionalOnProperty` prefix structure)
- Test patterns (pure unit tests vs Spring Boot integration tests)

Identify the established convention for:
- **Command handler registration style** (Step 3b)
- **REST API exposure** (Step 4)
- **Feature flags** (Step 5)

## Step 1: Understand the Input

Input can arrive in many forms. Extract these elements regardless of format:

| Element                  | What to extract                                                      |
|--------------------------|----------------------------------------------------------------------|
| **Command**              | Name, fields, which field identifies the consistency boundary        |
| **Events**               | Names, fields, which events this command produces                    |
| **Business rules**       | Preconditions, invariants, idempotency behavior                      |
| **State needed**         | What prior events must be replayed to evaluate rules                 |
| **Consistency boundary** | Single tag (one stream) or multi-tag (DCB across streams)            |

### Input: Specification / Natural Language

Extract command name, events, and business rules directly from the description.

### Input: Existing Tests

Analyze test file to understand expected behavior: commands sent, events asserted, failure cases.

### Input: Event Modeling Artifact

The write slice (blue stripe) shows: Command on left, Events on right, State (read model) below.

**Optionally**, the slice details may contain:
- `## Business Rules` — invariants and preconditions for `decide()` implementation
- `## Scenarios (GWTs)` — Given-When-Then acceptance criteria

When GWT scenarios are present, each numbered scenario maps 1:1 to a test method. Fields in element
blocks are **only rule-relevant** — fill remaining constructor params with test fixture values.

If the slice details contain `## Implementation Guidelines`, **follow them**.

### Input: Axon Framework 4 Code

```
AF4                               AF5 (Java)
────────────────────────────────  ─────────────────────────────────────
@Aggregate                        @EventSourced(tagKey = "...")  entity
@AggregateIdentifier              tag field annotated with @EventTag
@CommandHandler on aggregate      separate @Component handler class
@EventSourcingHandler             @EventSourcingHandler (different pkg)
commandGateway.sendAndWait()      commandGateway.send() → CompletableFuture
@MetaDataValue("key")             @MetadataValue("key")
```

**If requirements are unclear, ask the user before proceeding.**

### Determine Interaction Trigger

If the input does not make clear how the command will be triggered, ask:

> How will this command be triggered?
> - **REST API** — exposed via HTTP endpoint (add Presentation section + REST test)
> - **Automation only** — dispatched internally by an event handler (no REST endpoint)
> - **Both** — exposed via REST API and also dispatched by automations

## Step 2: Choose the AF5 Pattern

**Spring Boot** — entity and handler auto-discovered by Spring:

- `@EventSourced(tagKey = "tagName")` on entity (single tag)
- `@Component` on handler class
- Auto-registered by Spring Boot component scanning
- Tested with `@SpringBootTest` + `@Autowired AxonTestFixture`
- **Default choice** when the project uses Spring Boot

**Explicit Registration** — entity and handler registered manually via `@Configuration`:

- `@EventSourcedEntity` on entity
- `@EventCriteriaBuilder` static method on entity
- `@Configuration` class with `EntityModule` + `CommandHandlingModule` beans
- Handler class is NOT `@Component`
- Tested with non-Spring unit test (`AxonTestFixture.configSlice(...)`)
- Use when: unit tests without Spring context are explicitly required

Both patterns support single-tag and multi-tag (DCB).

See [references/af5-write-slice-patterns.md](references/af5-write-slice-patterns.md) for complete examples.

## Step 3: Implement the Domain (decide + evolve)

New slices live under `src/main/java/de/eventmodelers/slices/{context}/write/{slicename}/`.

### Command record

```java
// File: {slicename}/{SliceName}Command.java
@Command(namespace = "{Context}", name = "{SliceName}", version = "1.0.0")
public record {SliceName}Command(
    String {tagProperty},       // identifies the aggregate stream
    String field1,              // all fields from the slice definition
    int    field2
) {}
```

- Import `@Command` from `org.axonframework.messaging.commandhandling.annotation.Command`
- Use `String` for IDs/text, `int`/`long`/`double` for numbers, `boolean` for flags
- No `@TargetAggregateIdentifier` (AF5 doesn't use it)

### State record

```java
// File: {slicename}/{SliceName}Aggregate.java  — package-private
record {SliceName}State(boolean created /* only fields needed by decide() */) {

    static {SliceName}State initial() {
        return new {SliceName}State(false);
    }

    // Java records have no copy() — add explicit "wither" methods
    {SliceName}State withCreated(boolean created) {
        return new {SliceName}State(created);
    }
}
```

### decide()

```java
// package-private static method in {SliceName}Aggregate.java
static List<{Context}Event> decide({SliceName}Command cmd, {SliceName}State state) {
    if (state.created()) {
        return List.of();               // idempotent no-op
    }
    return List.of(new {EventName}(
        cmd.{tagProperty}(),
        cmd.field1(),
        cmd.field2()
    ));
}
```

Rules:
- **Pure function**: takes `(command, state)` → events. No side effects, no injected services.
- `throw new IllegalStateException(...)` for rule violations
- `return List.of()` for idempotent no-ops
- Everything decide needs MUST come from the command or state — never inject services here

### evolve()

```java
static {SliceName}State evolve({SliceName}State state, {Context}Event event) {
    return switch (event) {
        case {EventName} e      -> state.withCreated(true);
        case {OtherEvent} e     -> state;   // no-op: doesn't affect this slice's state
        // Every sealed subtype must have a branch — never use default
    };
}
```

**⚠️ ABSOLUTE RULE: NEVER add `default` to `evolve()`'s switch.**
Java 21 pattern-matching switch on sealed interfaces is exhaustive — the compiler enforces that every
subtype has a branch. Adding `default` removes that safety net. Before writing `evolve()`:

1. Find and read the bounded context's sealed event interface
2. List ALL concrete subtypes in the `permits` clause
3. Write an explicit `case` branch for EVERY subtype

**Exception**: `default -> state` IS allowed when the event interface is NOT sealed (e.g., a project-wide
root `DomainEvent` interface shared across modules). In that case, list all events you actually subscribe
to explicitly, with `default -> state` as a fallback for other events.

### ⚠️ Stateless commands

If `decide()` needs no prior state (command is always valid regardless of history):

```java
// No entity class needed. Handler signature: (command, metadata, eventAppender)
@Component
@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")
public class {SliceName}Handler {

    @CommandHandler
    public void handle({SliceName}Command command, AxonMetadata metadata, EventAppender eventAppender) {
        var events = List.of(new {EventName}(command.{tagProperty}(), command.field1()));
        eventAppender.append(events, metadata);
    }
}
```

## Step 3b: Command Handler Registration

Check the target project's existing slices for the established style. If no pattern exists, see
[references/command-handler-styles.md](references/command-handler-styles.md) for all three styles
(separate `@Component`, colocated handler on entity, explicit `@Configuration`).

**Default: Style 1 — separate `@Component` class:**

```java
// Entity
@EventSourced(tagKey = "{tagProperty}")
@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")
public class {SliceName}Entity {

    private {SliceName}State state;

    private {SliceName}Entity({SliceName}State state) { this.state = state; }

    @EntityCreator
    public static {SliceName}Entity create() {
        return new {SliceName}Entity({SliceName}State.initial());
    }

    // @EventSourcingHandler ONLY for events that actually mutate state
    @EventSourcingHandler
    public {SliceName}Entity on({EventName} event) {
        return new {SliceName}Entity(evolve(this.state, event));
    }

    {SliceName}State state() { return state; }   // package-private accessor
}

// Handler
@Component
@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")
public class {SliceName}Handler {

    @CommandHandler
    public void handle(
        {SliceName}Command command,
        AxonMetadata metadata,
        @InjectEntity {SliceName}Entity entity,
        EventAppender eventAppender
    ) {
        var events = decide(command, entity.state());
        eventAppender.append(events, metadata);
    }
}
```

In `@EventCriteriaBuilder` methods, `.andBeingOneOfTypes(...)` **MUST use `"Namespace.Name"` strings**
(e.g., `"Ordering.OrderPlaced"`). NEVER use `ClassName.class.getName()`. The type name is the
`@Event(namespace)` + `"."` + `@Event(name)`.

## Step 4: REST API Exposure (Optional)

Skip if the command is triggered only by automations.

```java
// File: {slicename}/{SliceName}RestController.java
@RestController
@ConditionalOnProperty(prefix = "slices.{context}.write", name = "{slicename}.enabled")
public class {SliceName}RestController {

    private final CommandGateway commandGateway;

    public {SliceName}RestController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping("/api/{context}/{tagProperty}")
    public Mono<ResponseEntity<Void>> handle(
        @PathVariable String {tagProperty},
        @RequestBody {SliceName}RequestBody body
    ) {
        var command = new {SliceName}Command({tagProperty}, body.field1(), body.field2());
        return Mono.fromFuture(commandGateway.send(command))
            .map(ignored -> ResponseEntity.ok().<Void>build())
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().<Void>build()));
    }

    public record {SliceName}RequestBody(String field1, int field2) {}
}
```

See [references/rest-api-patterns.md](references/rest-api-patterns.md) for REST controller and
`MockMvc` / `WebTestClient` test examples.

## Step 4a: Ensure Events Exist

Before implementing the slice, check `de.eventmodelers.slices.{context}.events`. If events don't
exist yet, create them **first**.

### Event hierarchy

```
DomainEvent (optional project root marker)
  └─ {Context}Event    ← sealed interface per bounded context
       └─ {EventName}  ← concrete record
```

### Context event interface (if it doesn't exist)

```java
// File: events/{Context}Event.java
package de.eventmodelers.slices.{context}.events;

public sealed interface {Context}Event permits {Event1}, {Event2} {}
```

If the interface already exists, add the new event name(s) to `permits`.

Also ensure the tag constant exists (add to a project-wide `EventTags` class or inline):

```java
// EventTags.java (create once per project)
public final class EventTags {
    public static final String {TAG_CONSTANT} = "{tagProperty}";
    // ...
}
```

### Concrete event records

```java
// File: events/{EventName}.java
@Event(namespace = "{Context}", name = "{EventName}", version = "1.0.0")
public record {EventName}(
    @EventTag(EventTags.{TAG_CONSTANT})
    String {tagProperty},           // aggregate identity field — always first
    String field1,
    int    field2
) implements {Context}Event {}
```

- Import `@Event` from `org.axonframework.messaging.eventhandling.annotation.Event`
- Import `@EventTag` from `org.axonframework.eventsourcing.annotation.EventTag`
- `namespace` = context name, `name` = record name, `version` = `"1.0.0"` for new events
- `@EventTag` on the sealed interface means ALL implementing events inherit the tag automatically;
  add it on individual record fields only for cross-stream DCB

## Step 5: Feature Flags (Optional)

Check the target project's convention first. See
[references/feature-flag-patterns.md](references/feature-flag-patterns.md) for the full
`@ConditionalOnProperty` example (entity, handler, REST controller, `application.properties`,
`additional-spring-configuration-metadata.json`) and alternatives.

**If `@ConditionalOnProperty` is used, update ALL of these files:**

- `src/main/resources/application.properties` — `slices.{context}.write.{slicename}.enabled=true`
- `src/test/resources/application.properties` — `slices.{context}.write.{slicename}.enabled=false`
- `META-INF/additional-spring-configuration-metadata.json` — add property entry

## Step 6: Implement Tests

### 6a. Unit tests (domain logic — no Spring context)

Call `decide()` and `evolve()` directly. These are pure functions — no framework needed.

```java
// File: src/test/java/de/eventmodelers/slices/{context}/write/{slicename}/{SliceName}Test.java
class {SliceName}Test {

    @Test
    @DisplayName("given empty state, when {sliceName}, then {eventName} emitted")
    void happyPath() {
        var state   = {SliceName}State.initial();
        var command = new {SliceName}Command("id-1", "value1", 42);

        var events = decide(command, state);

        assertThat(events).containsExactly(new {EventName}("id-1", "value1", 42));
    }

    @Test
    @DisplayName("given already created, when {sliceName}, then idempotent")
    void idempotent() {
        var state = evolve({SliceName}State.initial(), new {EventName}("id-1", "v", 0));
        var command = new {SliceName}Command("id-1", "v", 0);

        var events = decide(command, state);

        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("given invalid state, when {sliceName}, then throws")
    void ruleViolation() {
        // set up state that violates a rule
        var state   = /* ... */;
        var command = new {SliceName}Command("id-1", "v", 0);

        assertThatThrownBy(() -> decide(command, state))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("...");
    }
}
```

**Implement ALL GWT scenarios from the slice definition, not just the happy path.**

Cover for every specification:
- **Happy path**: no prior events → expected events produced
- **Idempotency**: duplicate command → empty list
- **Rule violations**: invalid state → `IllegalStateException`
- **All mutating evolve branches**: for every event subtype that changes state in `evolve()`,
  add at least one test proving that transition works

### 6b. REST API Tests (only if Step 4 chosen REST)

See [references/rest-api-patterns.md](references/rest-api-patterns.md) for `@WebMvcTest` +
`MockMvc` examples.

## References

- [AF5 Write Slice Patterns](references/af5-write-slice-patterns.md) — Complete Java examples
  (Spring Boot + Explicit Registration, single-tag and multi-tag DCB)
- [Command Handler Styles](references/command-handler-styles.md) — All three registration styles
- [REST API Patterns](references/rest-api-patterns.md) — REST controller and test examples
- [Feature Flag Patterns](references/feature-flag-patterns.md) — `@ConditionalOnProperty` and alternatives