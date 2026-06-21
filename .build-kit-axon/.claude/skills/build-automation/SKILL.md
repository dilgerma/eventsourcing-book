---
name: build-automation
authors:
  - Mateusz Nowak
  - Martin Dilger
description: >
  Implement automation slices (Event to Command) using Axon Framework 5, Vertical Slice Architecture,
  and Event Modeling patterns. An automation is: an Event Handler that reacts to an event by dispatching
  a command via CommandDispatcher. Automations can be stateless (direct event-to-command mapping) or
  with a private read model (to look up data needed for command construction).
  Use when: (1) implementing a new automation / event-to-command reactor in an AF5 Java project,
  (2) migrating/porting an automation from Axon Framework 4 (Java or Kotlin) to AF5,
  (3) user provides a specification, Event Modeling artifact, or natural language description of an
  event-to-command reaction and asks to implement it,
  (4) user says "implement", "create", "add", "migrate", "port" an automation, event handler,
  reactor, or event-to-command flow in an Axon Framework 5 / Vertical Slice Architecture project.
  Understands AF4 @EventHandler/@ProcessingGroup input as one possible source format.
---

# Axon Framework 5 — Automation Slice (Java)

An automation reacts to an event by dispatching a command. In Event Modeling: the **orange** stripe.

There are two kinds:
- **Stateless**: Direct event-to-command mapping — no stored state needed
- **With read model**: Needs a private read model to look up data required for command construction
  (e.g., iterate over all entities matching a category)

## Step 0: Discover Target Project Conventions

> **Comments & description**: Each element in the slice carries a `comments: string[]` array and a `description` field. Use these as implementation hints. When done, resolve each used comment: `POST <BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/<nodeId>/comments/<commentId>/resolve` (get IDs first via GET on same path).

Read the target project's `CLAUDE.md` and explore existing slices. Look for:

- File splitting conventions (one Java file per class vs inner classes)
- Visibility conventions
- Metadata keys (`@MetadataValue` fields in existing processors)
- Feature flag patterns (Step 4 — optional)
- Spring Boot test annotation: check if the project defines a meta-annotation over `@SpringBootTest`

## Step 1: Understand the Input

Extract these elements regardless of input format:

| Element                | What to extract                                                        |
|------------------------|------------------------------------------------------------------------|
| **Trigger event**      | Which event triggers the automation, and which condition filters it    |
| **Target command**     | Which command to dispatch, with what properties                        |
| **Mapping logic**      | How event properties map to command properties                         |
| **Strategy/calculator**| Any injectable strategy for deriving command properties from event data|
| **Metadata**           | Which metadata keys to propagate from event to command                 |
| **Read model needed?** | Does the automation need data NOT in the trigger event itself?         |

If the Event Modeling artifact includes slice details with `## Scenarios (GWTs)`, use them to derive
test cases. GWT format for automations: `Given (events) → Then (command | NOTHING)`. Events in
Given include read-model-building events first, trigger event last.

If the slice details contain `## Implementation Guidelines`, **follow them**.

### Stateless vs With Read Model Decision

Choose **with read model** when:
- The automation needs data that is NOT in the trigger event (e.g., "find all entities of type X")
- The automation must iterate over a collection to dispatch multiple commands
- Two different events are involved: one builds the read model, another triggers the dispatch

Choose **stateless** when:
- All command fields can be derived directly from the trigger event + metadata
- The mapping is direct or uses a pure calculation/strategy

### Input: Axon Framework 4 Code

```
AF4                                  AF5 (Java)
─────────────────────────────────    ─────────────────────────────────────
@ProcessingGroup("name")             (not needed — Spring Boot auto-config)
@DisallowReplay                      (not needed in AF5)
@Component                           @Component
@EventHandler                        @EventHandler (different package)
commandGateway.sendAndWait(cmd, m)   commandDispatcher.send(cmd, metadata)
CommandGateway (constructor-inject)  CommandDispatcher (method parameter)
@MetaDataValue("key")                @MetadataValue("key")
```

**If requirements are unclear, ask the user before proceeding.**

## Step 2: Ensure Events Exist

All events the automation handles must exist. If they don't, create them **first** following
`build-state-change` Step 4a (sealed event interface + concrete event records).

## Step 3: Implement the Automation

### CommandDispatcher vs CommandGateway

**Always use `CommandDispatcher`** to dispatch commands from within `@EventHandler` methods:

- `CommandDispatcher` is AF5's preferred way to send commands from within message handlers
- It is **ProcessingContext-scoped** — inject it as a **method parameter** on the `@EventHandler`,
  NEVER as a constructor parameter
- `CommandGateway` is a singleton intended for external callers (REST controllers, etc.)

```java
// CORRECT: CommandDispatcher as method parameter
@EventHandler
public void react({TriggerEvent} event, CommandDispatcher commandDispatcher) {
    commandDispatcher.send(command, metadata);
}

// WRONG: CommandGateway injected via constructor
public class MyProcessor {
    private final CommandGateway commandGateway;  // DON'T DO THIS in processors
    ...
}
```

### Error Propagation with CompletableFuture

Return a `CompletableFuture` from the `@EventHandler` method so AF5 awaits command completion.
If a command fails, the event handler fails and the event processor retries.

- `commandDispatcher.send(command, metadata)` returns a `CommandResult`
- `CommandResult.resultMessage()` returns a `CompletableFuture`
- For multiple commands: `CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))`

---

### Stateless Automation

New automation slices live under
`src/main/java/de/eventmodelers/slices/{context}/automation/{slicename}/`.

#### Strategy interface (if needed)

A `@FunctionalInterface` for injectable logic deriving command properties from event data:

```java
@FunctionalInterface
public interface {StrategyName} {
    {OutputType} calculate({InputType} input);
}
```

Skip if the mapping from event to command is trivial and direct.

#### Configuration (if strategy exists)

```java
@ConditionalOnProperty(prefix = "slices.{context}.automation", name = "{slicename}.enabled")
@Configuration
class {AutomationName}Configuration {

    @Bean
    public {StrategyName} {strategyName}() {
        return input -> /* default implementation */;
    }
}
```

#### Processor

```java
@Component
@ConditionalOnProperty(prefix = "slices.{context}.automation", name = "{slicename}.enabled")
public class {AutomationName}Processor {

    private final {StrategyName} strategy;   // omit if no strategy

    public {AutomationName}Processor({StrategyName} strategy) {
        this.strategy = strategy;
    }

    @EventHandler
    public void react(
        {TriggerEvent} event,
        @MetadataValue("{correlationKey}") String correlationId,
        CommandDispatcher commandDispatcher
    ) {
        if (!shouldReact(event)) {
            return;
        }
        var command  = new {TargetCommand}Command(event.{tagProperty}() /*, mapped fields */);
        var metadata = AxonMetadata.with("{correlationKey}", correlationId);
        commandDispatcher.send(command, metadata);
    }

    private boolean shouldReact({TriggerEvent} event) {
        return true; // replace with actual condition from slice definition
    }
}
```

---

### Automation with Read Model

When the automation needs stored state, put everything in one package. **Never reuse another
slice's read model** — build a private one.

#### Read model record (package-private)

```java
// File: {AutomationName}Entry.java — package-private
record {AutomationName}Entry(String entityId, String filterField) {}
```

Or use a JPA entity if the project has Spring Data JPA.

#### Processor with private read model

```java
@Component
@ConditionalOnProperty(prefix = "slices.{context}.automation", name = "{slicename}.enabled")
// MetadataSequencingPolicy: events for the same correlation unit processed sequentially —
// prevents race conditions when the read model is being built and consumed concurrently
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = "{correlationKey}")
public class {AutomationName}Processor {

    // In-memory private read model — indexed by entity id
    private final Map<String, {AutomationName}Entry> store = new ConcurrentHashMap<>();

    // Phase 1 — build the private read model from setup events
    @EventHandler
    public void on(
        {SetupEvent} event,
        @MetadataValue("{correlationKey}") String correlationId
    ) {
        store.put(event.{tagProperty}(), new {AutomationName}Entry(
            event.{tagProperty}(),
            event.filterField()
        ));
    }

    // Phase 2 — trigger: dispatch a command per matching entry
    @EventHandler
    public CompletableFuture<Void> react(
        {TriggerEvent} event,
        @MetadataValue("{correlationKey}") String correlationId,
        CommandDispatcher commandDispatcher
    ) {
        var futures = store.values().stream()
            .filter(entry -> entry.filterField().equals(event.filterValue()))
            .map(entry -> {
                var command  = new {TargetCommand}Command(entry.entityId() /*, other fields */);
                var metadata = AxonMetadata.with("{correlationKey}", correlationId);
                return commandDispatcher.send(command, metadata).resultMessage();
            })
            .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
```

Key rules:
- **`@SequencingPolicy(MetadataSequencingPolicy, "{correlationKey}")`** — sequential processing per
  correlation unit prevents race conditions on the private read model
- **`CommandDispatcher` as method parameter** — ProcessingContext-scoped, never constructor-injected
- **`CompletableFuture<Void>` return** on the trigger handler — `allOf(...)` awaits all commands;
  if any fails, the event handler fails and the processor retries
- **Two `@EventHandler` methods in one class**: one builds, one reacts
- **Private read model belongs to this automation only** — never share it with other slices

## Step 4: Feature Flags (Optional)

Check the target project's convention. See
[references/feature-flag-patterns.md](references/feature-flag-patterns.md).

Add to ALL config files when using `@ConditionalOnProperty`:
- `application.properties` — `slices.{context}.automation.{slicename}.enabled=true`
- `application.properties` (test) — `slices.{context}.automation.{slicename}.enabled=false`
- `META-INF/additional-spring-configuration-metadata.json` — add entry

**Enable BOTH the automation AND its target write slice** in tests.

## Step 5: Implement Tests

**For stateless automations** — pure unit tests with a mocked `CommandDispatcher`:

```java
// File: src/test/java/de/eventmodelers/slices/{context}/automation/{slicename}/{AutomationName}Test.java
class {AutomationName}ProcessorTest {

    private {AutomationName}Processor processor;

    @Mock
    private CommandDispatcher commandDispatcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new {AutomationName}Processor(/* strategy */);
    }

    @Test
    @DisplayName("given trigger event with condition met, then command dispatched")
    void happyPath() {
        var event = new {TriggerEvent}("entity-1" /*, fields that meet condition */);

        processor.react(event, "correlation-1", commandDispatcher);

        verify(commandDispatcher).send(
            eq(new {TargetCommand}Command("entity-1" /*, expected fields */)),
            any()
        );
    }

    @Test
    @DisplayName("given trigger event with condition not met, then no command dispatched")
    void conditionNotMet() {
        var event = new {TriggerEvent}("entity-1" /*, fields that do NOT meet condition */);

        processor.react(event, "correlation-1", commandDispatcher);

        verifyNoInteractions(commandDispatcher);
    }
}
```

**For automations with read model:**

```java
class {AutomationName}ProcessorTest {

    private {AutomationName}Processor processor;

    @Mock
    private CommandDispatcher commandDispatcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new {AutomationName}Processor();
    }

    @Test
    @DisplayName("given setup events then trigger, then command dispatched for matching entries")
    void happyPath() {
        processor.on(new {SetupEvent}("entity-1", "filter-A"), "corr-1");
        processor.on(new {SetupEvent}("entity-2", "filter-B"), "corr-1");

        processor.react(new {TriggerEvent}("filter-A"), "corr-1", commandDispatcher);

        verify(commandDispatcher, times(1)).send(
            eq(new {TargetCommand}Command("entity-1" /*, fields */)), any()
        );
        verifyNoMoreInteractions(commandDispatcher);
    }

    @Test
    @DisplayName("given no setup events, then no commands dispatched")
    void noSetupEvents() {
        processor.react(new {TriggerEvent}("filter-X"), "corr-1", commandDispatcher);

        verifyNoInteractions(commandDispatcher);
    }
}
```

### Important notes for read model tests

1. **Isolate tests using entity ID filtering** if using a shared `RecordingCommandBus`:
   generate unique IDs per test method, then filter assertions to the current test's IDs.

2. **Use `containsExactlyInAnyOrder`** when order is non-deterministic (map/set iteration).

3. **Put all events in the test** in the order they would arrive in production — setup events
   before trigger events.

### Test Cases to Cover

**Stateless automations:**
1. Condition met → expected command dispatched
2. Condition not met → no commands dispatched

**Automations with read model:**
1. Setup + trigger with matching filter → commands dispatched for matching entries only
2. Setup + trigger with non-matching filter → no commands dispatched
3. Temporal ordering: setup before trigger vs setup after trigger → only entries that existed
   at trigger time receive a command

### Mapping GWT Scenarios to Tests

| GWT Element | Test Code |
|---|---|
| Event in Given | `processor.on(new Event(...), correlationId)` |
| Multiple events in Given | multiple `on(...)` calls — setup events first, trigger last |
| Command in Then | `verify(commandDispatcher).send(eq(expectedCommand), any())` |
| NOTHING in Then | `verifyNoInteractions(commandDispatcher)` |

## References

- [Stateless Automation Example](references/automation-test-example.md) — Complete Java test example
- [Automation with Read Model Example](references/automation-with-read-model-test-example.md) — Multi-command test
- [Feature Flag Patterns](references/feature-flag-patterns.md) — `@ConditionalOnProperty` and alternatives