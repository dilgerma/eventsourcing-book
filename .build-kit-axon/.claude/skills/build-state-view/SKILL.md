---
name: build-state-view
authors:
  - Mateusz Nowak
  - Martin Dilger
description: >
  Implement read slices (projections + query handlers + REST API + tests) using Axon Framework 5
  with Spring Boot. A read slice is: Events projected into a Read Model, queried via QueryGateway.
  Use when: (1) implementing a new read slice / projection in an AF5 Java project,
  (2) migrating/porting a read slice from Axon Framework 4 (Java or Kotlin) to AF5,
  (3) user provides a read slice specification or Event Modeling artifact and asks to implement it,
  (4) user says "implement", "create", "add" a read slice, projection, query handler,
  or read model in an Axon Framework 5 / Vertical Slice Architecture project.
---

# Axon Framework 5 — Read Slice (Java)

## Step 0: Discover Target Project Conventions

> **Comments & description**: Each element in the slice carries a `comments: string[]` array and a `description` field. Use these as implementation hints. When done, resolve each used comment: `POST <BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/<nodeId>/comments/<commentId>/resolve` (get IDs first via GET on same path).

Before writing any code, read the target project's `CLAUDE.md` and explore at least one existing
read slice under `de.eventmodelers.slices`. Look for:

- Feature flag pattern (`@ConditionalOnProperty` prefix structure)
- Assertion library (AssertJ, etc.)
- Whether projections use in-memory (`ConcurrentHashMap`) or JPA — default to **in-memory**
  unless Spring Data JPA is present in the pom
- Spring WebFlux vs MVC (this project uses WebFlux — `Mono`/`Flux` return types)
- Metadata keys attached to events (`@MetadataValue` fields in existing projectors)

Also identify the established convention for:
- **REST API exposure** (Step 4 — optional)
- **Feature flags** (Step 3 — optional)

## Step 1: Ensure Events Exist

Before implementing the read slice, verify that all events the projector handles exist in the
codebase. If they don't, create them **first**.

### Event hierarchy

```
DomainEvent (optional project root marker)
  └─ {Context}Event    ← sealed interface per bounded context
       └─ {EventName}  ← concrete record
```

### Context event interface (if it doesn't exist)

```java
// File: de/eventmodelers/slices/{context}/events/{Context}Event.java
public sealed interface {Context}Event permits {Event1}, {Event2} {}
```

### Concrete event records

```java
@Event(namespace = "{Context}", name = "{EventName}", version = "1.0.0")
public record {EventName}(
    @EventTag(EventTags.{TAG_CONSTANT})
    String {tagProperty},
    String field1
) implements {Context}Event {}
```

Key rules:
- Import `@Event` from `org.axonframework.messaging.eventhandling.annotation.Event`
- `namespace` = context name, `name` = record name, `version` = `"1.0.0"` for new events
- When an event participates in a DCB, add extra `@EventTag` on cross-stream fields

## Step 2: Implement the Read Slice

If the Event Modeling artifact includes slice details with `## Scenarios (GWTs)`, use them to
derive test cases. GWT format for read slices: `Given (events) → Then (information)` — no When.
Events in Given tell you which events the projector handles. The information element in Then
describes the expected query result.

If the slice details contain `## Implementation Guidelines`, **follow them**.

### Query annotation

Every query record must have `@Query(namespace, name, version)`:

```java
@Query(namespace = "{Context}", name = "Get{SliceName}", version = "1.0.0")
public record Get{SliceName}(String {filterField}) {
    public record Result(List<{SliceName}Summary> items) {}
}
```

- Import `@Query` from `org.axonframework.messaging.queryhandling.annotation.Query`

A read slice lives in a single package. **Do NOT add Domain/Application/Presentation section
comments** — those are only for write slices.

### Slice package structure

```
de/eventmodelers/slices/{context}/read/{slicename}/
├── Get{SliceName}.java       ← query record + nested Result
├── {SliceName}Summary.java   ← read model (projection output shape)
├── {SliceName}Projector.java ← @Component with @EventHandler + @QueryHandler
└── {SliceName}RestApi.java   ← @RestController (if REST chosen)
```

### In-memory projector + query handler

```java
// File: {SliceName}Projector.java
@Component
@ConditionalOnProperty(prefix = "slices.{context}.read", name = "{slicename}.enabled")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = "{correlationKey}")
public class {SliceName}Projector {

    // ConcurrentHashMap: safe for concurrent reads and single-threaded @EventHandler writes
    private final Map<String, {SliceName}Summary> store = new ConcurrentHashMap<>();

    @EventHandler
    public void on({CreationEvent} event) {
        store.put(event.{tagProperty}(), new {SliceName}Summary(
            event.{tagProperty}(),
            event.field1()
            // map all fields from the event to the read model
        ));
    }

    @EventHandler
    public void on({UpdateEvent} event) {
        var existing = store.get(event.{tagProperty}());
        if (existing == null) return;
        store.put(event.{tagProperty}(), new {SliceName}Summary(
            existing.id(),
            event.updatedField()
        ));
    }

    @EventHandler
    public void on({DeletionEvent} event) {
        store.remove(event.{tagProperty}());
    }

    @QueryHandler
    public Get{SliceName}.Result handle(Get{SliceName} query) {
        var items = store.values().stream()
            .filter(item -> item.{filterField}().equals(query.{filterField}()))
            .toList();
        return new Get{SliceName}.Result(items);
    }
}
```

### Result DTO rules

- If the read model matches the query result **1:1**, expose the summary record directly.
- If the read model contains fields the caller already knows from the query (e.g., the filter field),
  omit those from the `Result` and map from the projector's internal model.

### JPA option (if Spring Data JPA is in the pom)

If JPA is available, prefer it over in-memory for persistence across restarts:

```java
@Entity
@Table(
    name = "{context}_read_{slicename}",
    indexes = {@Index(name = "idx_{context}_{slicename}_{col}", columnList = "{filterField}")}
)
public class {SliceName}Entity {
    @Id private String id;
    private String {filterField};
    // ... other fields
}

@Repository
@ConditionalOnProperty(prefix = "slices.{context}.read", name = "{slicename}.enabled")
interface {SliceName}Repository extends JpaRepository<{SliceName}Entity, String> {
    List<{SliceName}Entity> findAllBy{FilterField}(String {filterField});
}
```

Use `findAllBy{FilterField}(...)` in the `@QueryHandler` — DB-level filtering, not client-side.

## Step 3: Feature Flags (Optional)

Check the target project's convention first. See
[references/feature-flag-patterns.md](references/feature-flag-patterns.md) for the full
`@ConditionalOnProperty` example and alternatives.

Update ALL of these files when using `@ConditionalOnProperty`:
- `src/main/resources/application.properties` — `slices.{context}.read.{slicename}.enabled=true`
- `src/test/resources/application.properties` — `slices.{context}.read.{slicename}.enabled=false`
- `META-INF/additional-spring-configuration-metadata.json` — add property entry

## Step 4: REST API Exposure (Optional)

Check the target project's convention first.

```java
// File: {SliceName}RestApi.java
@RestController
@ConditionalOnProperty(prefix = "slices.{context}.read", name = "{slicename}.enabled")
public class {SliceName}RestApi {

    private final QueryGateway queryGateway;

    public {SliceName}RestApi(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/api/{context}/{filterField}")
    public Mono<Get{SliceName}.Result> query(@PathVariable String {filterField}) {
        return Mono.fromFuture(
            queryGateway.query(new Get{SliceName}({filterField}), Get{SliceName}.Result.class)
        );
    }
}
```

See [references/rest-api-patterns.md](references/rest-api-patterns.md) for `WebTestClient` test examples.

## Step 5: Design Test Cases

Cover these scenarios (adapt to the specific slice):

1. **Empty state**: No events → query returns empty result
2. **Single entity**: One creation event → query returns single item
3. **Multiple entities**: Multiple creation events → query returns all items
4. **State updates**: Creation + update event → query returns updated state
5. **Aggregation**: Same entity updated multiple times → values accumulated correctly
6. **Deletion**: Entity added then removed → disappears from result
7. **Isolation**: Multiple entities exist → query returns only matching ones

### Mapping GWT Scenarios to Tests

| GWT Element | Test Code |
|---|---|
| `NOTHING` in Given | instantiate projector, call `handle(query)` directly |
| Event in Given | call `projector.on(event)` |
| Information in Then | `assertThat(result.items()).containsExactlyInAnyOrder(...)` |

## Step 6: Implement the Slice Test

Pure unit tests — instantiate the projector directly, no Spring context needed.
Fast, no container startup.

```java
// File: src/test/java/de/eventmodelers/slices/{context}/read/{slicename}/{SliceName}ProjectorTest.java
class {SliceName}ProjectorTest {

    private {SliceName}Projector projector;

    @BeforeEach
    void setUp() {
        projector = new {SliceName}Projector();
    }

    @Test
    @DisplayName("given no events, when query, then empty result")
    void emptyState() {
        var result = projector.handle(new Get{SliceName}("filter-value"));

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("given creation event, then item appears in result")
    void creationEvent() {
        projector.on(new {CreationEvent}("id-1", "filter-value" /*, other fields */));

        var result = projector.handle(new Get{SliceName}("filter-value"));

        assertThat(result.items()).containsExactly(
            new {SliceName}Summary("id-1", "filter-value" /*, expected fields */)
        );
    }

    @Test
    @DisplayName("given creation then deletion, then item disappears")
    void deletionEvent() {
        projector.on(new {CreationEvent}("id-1", "filter-value"));
        projector.on(new {DeletionEvent}("id-1"));

        var result = projector.handle(new Get{SliceName}("filter-value"));

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("items are isolated by filter field")
    void isolation() {
        projector.on(new {CreationEvent}("id-1", "group-A"));
        projector.on(new {CreationEvent}("id-2", "group-B"));

        assertThat(projector.handle(new Get{SliceName}("group-A")).items()).hasSize(1);
        assertThat(projector.handle(new Get{SliceName}("group-B")).items()).hasSize(1);
    }
}
```

### Key Rules

- **Metadata when needed**: If the projector uses `@MetadataValue(...)`, pass metadata by publishing
  events to an `AxonTestFixture` rather than calling `on(event)` directly. Use the Spring Boot
  integration test approach in that case.
- **Assert with full objects**: Use `containsExactlyInAnyOrder(new Summary(...))` rather than
  field-by-field assertions — catches serialization mismatches.

## Step 7: REST API Test (Optional, only if Step 4 chosen REST)

See [references/rest-api-patterns.md](references/rest-api-patterns.md) for a `WebTestClient` +
mocked `QueryGateway` example.

## References

- [Read Slice Test Example](references/read-slice-test-example.md) — Complete working example
- [REST API Patterns](references/rest-api-patterns.md) — REST controller and test examples
- [Feature Flag Patterns](references/feature-flag-patterns.md) — `@ConditionalOnProperty` and alternatives