---
name: build-automation-workflow
authors:
  - Martin Dilger
description: >
  Analyze a list of automation slices and translate appropriate ones into Axon Framework 5 Workflows
  (long-running, durable, multi-step processes) instead of plain @EventHandler automations.
  Determines WHICH slices need a Workflow, then implements them using the AF5 Workflow engine.
  Use when: (1) given a set of automation slice.json definitions, decide which ones should be
  Workflows; (2) user asks to "convert automation to workf
  low", "implement as workflow",
  or "use Axon Workflow for this slice"; (3) a slice has multi-step logic, needs to wait for
  external input/approval, requires compensation/rollback, or spans significant time.
  NOTE: AF5 Workflows are in Preview — APIs may change; not intended for production use yet.
---

# Build Automation Workflow (Java / Axon Framework 5)

> **Preview feature** — AF5 Workflow APIs may change without notice and are not yet for production.

New workflow slices live under
`src/main/java/de/eventmodelers/slices/{context}/automation/{slicename}/`.

---

## Step 0: Discover Project Conventions

> **Comments & description**: Each element in the slice carries a `comments: string[]` array and a `description` field. Use these as implementation hints. When done, resolve each used comment: `POST <BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/<nodeId>/comments/<commentId>/resolve` (get IDs first via GET on same path).

Read `CLAUDE.md` and check whether the project already uses AF5 Workflows. Look for:

- A `WorkflowModule` bean in any `@Configuration` class
- Classes annotated with `@Workflow`
- Existing `*Workflow.java` files under any slice package
- The `WorkflowModule` dependency in `pom.xml`:

```xml
<dependency>
    <groupId>io.axoniq.framework</groupId>
    <artifactId>axoniq-workflows</artifactId>
    <!-- version managed by the BOM -->
</dependency>
```

If the module is missing from the pom, add it before implementing any workflow.

---

## Step 1: Analyze Automation Slices — Which Ones Need a Workflow?

Read every slice.json under `.build-kit-axon/.slices/{context}/` where `processors` is not empty,
or where the user specifies a list of slices to analyze.

For each automation slice, apply this decision table:

| Signal in the slice definition | Decision |
|---|---|
| Single trigger event → single command, no waiting | **Plain automation** (`build-automation`) |
| Multi-step: trigger → command → wait for event → another command | **Workflow** |
| Process must wait for human approval or external confirmation | **Workflow** |
| Compensation/rollback: if step N fails, undo steps 1..N-1 | **Workflow** |
| Steps involve a timer / scheduled delay | **Workflow** |
| Fan-out: one trigger dispatches to many entities then waits for all | **Workflow** |
| Parent-child: one process spawns another and waits for its completion | **Workflow** (sub-workflow) |
| Scatter-gather: send to many, process replies as they arrive | **Workflow** |

For each slice, output a classification:

```
Slice: "ProcessOrderFulfillment"   → WORKFLOW  (reason: multi-step with compensation)
Slice: "SendWelcomeEmail"          → AUTOMATION (reason: single-step fire-and-forget)
Slice: "ApproveExpenseReport"      → WORKFLOW  (reason: human approval with timeout/escalation)
```

Ask the user to confirm before proceeding if any classification is ambiguous.

---

## Step 2: Ensure Events and Commands Exist

For each workflow slice, verify all trigger events and dispatched commands exist. If they don't,
create them **first** following `build-state-change` Step 4a (sealed event interface + concrete
event records).

---

## Step 3: Implement the Workflow

### 3a — Annotated workflow class (default)

The simplest way to define a workflow using `@Workflow`:

```java
// File: {slicename}/{SliceName}Workflow.java
package de.eventmodelers.slices.{context}.automation.{slicename};

import org.axonframework.workflow.annotation.Workflow;
import org.axonframework.workflow.SimpleWorkflowContext;

@Workflow(
    idProperty       = "{triggerEventIdField}",    // field name on the trigger event that becomes the workflow ID
    startOnEventClass = {TriggerEvent}.class,       // event that starts a new workflow instance
    workflowNamespace = "de.eventmodelers.slices.{context}"
)
public class {SliceName}Workflow {

    /**
     * The main workflow method. Runs as a durable, resumable coroutine.
     * ctx.awaitExecute() blocks until the step completes or fails.
     * ctx.waitForEvent() suspends until an external event arrives.
     */
    public void execute(SimpleWorkflowContext ctx) {
        var orderId = (String) ctx.workflowPayload().get("{triggerEventIdField}");

        // Step 1: dispatch the first command and wait for it to complete
        ctx.awaitExecute("step1-{commandName}",
            payload("{field}", ctx.workflowPayload().get("{field}")).getValues(),
            {ServiceInterface}::{methodName});

        // Step 2: wait for a confirmation event before continuing
        var confirmation = ctx.waitForEvent("step2-await{EventName}",
            {ConfirmationEvent}.class,
            Duration.ofMinutes(30));

        if (confirmation.timeout()) {
            ctx.fail(new RuntimeException("No {ConfirmationEvent} received within 30 minutes"));
            return;
        }

        confirmation.await();   // blocks until event arrives (already arrived since !timeout())

        // Step 3: dispatch a second command based on the confirmation
        ctx.awaitExecute("step3-{secondCommandName}",
            payload("{field}", confirmation.result()).getValues(),
            {ServiceInterface}::{secondMethodName});
    }
}
```

### 3b — Key context API

| Method | What it does |
|---|---|
| `ctx.awaitExecute(stepName, payload, handler)` | Dispatches a command/action and **blocks** until completion |
| `ctx.execute(stepName, payload, handler, timeout, options)` | Dispatches **without blocking** — returns `WorkflowStepResult` immediately |
| `ctx.waitForEvent(stepName, EventClass.class, timeout)` | **Suspends** the workflow until the event arrives or the timeout fires |
| `ctx.awaitEvent(stepName, EventClass.class, timeout)` | Same as `waitForEvent` but also matches using an association predicate |
| `ctx.sleep(stepName, duration)` | Durably suspends for a duration (survives restarts) |
| `ctx.allMatch(predicate, steps...)` | Fan-in barrier: waits until all steps satisfy the predicate |
| `ctx.anyMatch(predicate, steps...)` | Scatter-gather: waits until any step satisfies the predicate |
| `ctx.setPayload(stepName, newPayload)` | Persists state to the durable workflow payload |
| `ctx.workflowId()` | The workflow instance ID (derived from the trigger event) |
| `ctx.workflowPayload()` | Current durable payload as `Map<String, Object>` |
| `ctx.fail(exception)` | Terminates the workflow with FAILED status |
| `ctx.cancel()` | Terminates the workflow with CANCELLED status |

The `handler` argument in `awaitExecute`/`execute` is a `BiFunction<ProcessingContext, Map<String, Object>, Map<String, Object>>` —
typically a Spring bean method reference: `MyService::doWork`.

### 3c — Service handler beans

Workflow step handlers are Spring beans registered in the `ProcessingContext`. For each step,
create a Spring service bean:

```java
// File: {slicename}/{SliceName}WorkflowServices.java
@Component
@ConditionalOnProperty(prefix = "slices.{context}.automation", name = "{slicename}.enabled")
public class {SliceName}WorkflowServices {

    private final CommandGateway commandGateway;

    public {SliceName}WorkflowServices(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    /** Step 1: reserve/validate. Returns result map for next steps. */
    public Map<String, Object> step1(ProcessingContext ctx, Map<String, Object> payload) {
        var command = new {SliceName}Command(
            (String) payload.get("{field}")
            // map other fields
        );
        commandGateway.sendAndWait(command);
        return Map.of();    // return data the next step needs, or empty map
    }

    /** Step 2: compensate step 1 on failure. */
    public Map<String, Object> compensateStep1(ProcessingContext ctx, Map<String, Object> payload) {
        var command = new Compensate{SliceName}Command((String) payload.get("{field}"));
        commandGateway.sendAndWait(command);
        return Map.of();
    }
}
```

### 3d — Register the workflow module

Add the `WorkflowModule` to the Spring configuration. **Create once per bounded context**
(not once per slice).

```java
// File: {context}/WorkflowConfiguration.java
@Configuration
@ConditionalOnProperty(prefix = "slices.{context}", name = "workflows.enabled")
public class {Context}WorkflowConfiguration {

    @Bean
    public EntityModule<?, ?> {context}WorkflowModule() {
        return WorkflowModule
            .usingContext(SimpleWorkflowContext.class)
            .workflowContextFactory(c -> new SimpleWorkflowContextFactory())
            .workflowExecutionFactory(c ->
                new DSLAdoptingExecutionFactory<>(SimpleWorkflowContext.class))
            .definitions(d -> d
                // autodetected: reads @Workflow annotations from registered beans
                .autodetected(c -> new {SliceName}Workflow(), SimpleWorkflowContext.class)
                // add more workflows in the same chain:
                // .autodetected(c -> new {OtherSlice}Workflow(), SimpleWorkflowContext.class)
            );
    }
}
```

---

## Step 4: Common Patterns

### Compensating transaction (saga)

```java
public void execute(SimpleWorkflowContext ctx) {
    var id = (String) ctx.workflowPayload().get("id");

    // Step 1
    ctx.awaitExecute("reserveStock",
        payload("id", id).getValues(),
        services::reserveStock);

    // Step 2 — compensate step 1 on failure
    try {
        ctx.awaitExecute("chargePayment",
            payload("id", id, "amount", ctx.workflowPayload().get("amount")).getValues(),
            services::charge);
    } catch (StepFailedException e) {
        ctx.awaitExecute("releaseStock",
            payload("id", id).getValues(),
            services::releaseStock);
        ctx.fail(new RuntimeException("Payment failed: " + e.getMessage()));
        return;
    }

    // Step 3 — compensate steps 2 and 1 on failure
    try {
        ctx.awaitExecute("shipOrder",
            payload("id", id).getValues(),
            services::shipOrder);
    } catch (StepFailedException e) {
        ctx.awaitExecute("refundPayment",
            payload("id", id).getValues(),
            services::refundPayment);
        ctx.awaitExecute("releaseStock",
            payload("id", id).getValues(),
            services::releaseStock);
        ctx.fail(new RuntimeException("Shipping failed: " + e.getMessage()));
    }
}
```

### Human-in-the-loop (approval with escalation)

```java
public void execute(SimpleWorkflowContext ctx) {
    // Request approval
    ctx.awaitExecute("requestApproval",
        payload("approver", "team-lead", "requestId", ctx.workflowId()).getValues(),
        services::sendApprovalRequest);

    // Wait up to 3 days for a response
    var decision = ctx.waitForEvent("awaitApproval",
        ApprovalDecisionEvent.class,
        Duration.ofDays(3));

    if (decision.timeout()) {
        // Escalate to department head
        ctx.awaitExecute("escalate",
            payload("approver", "department-head", "requestId", ctx.workflowId()).getValues(),
            services::escalate);

        decision = ctx.waitForEvent("awaitEscalatedApproval",
            ApprovalDecisionEvent.class,
            Duration.ofDays(1));

        if (decision.timeout()) {
            ctx.fail(new RuntimeException("No approval received after escalation"));
            return;
        }
    }

    decision.await();
    // decision.result() contains the ApprovalDecisionEvent payload
}
```

### Fan-out / Fan-in

```java
public void execute(SimpleWorkflowContext ctx) {
    var items = (List<Map<String, Object>>) ctx.workflowPayload().get("lineItems");

    // Fan-out: launch reservations concurrently
    var reservations = items.stream()
        .map(item -> ctx.execute(
            "reserve-" + item.get("sku"),
            payload("sku", item.get("sku"), "qty", item.get("quantity")).getValues(),
            services::reserveStock,
            Duration.ofSeconds(30),
            defaults()))
        .toArray(WorkflowStepResult[]::new);

    // Fan-in: wait for all
    var guard = ctx.allMatch(WorkflowStepResult::isCompleted, reservations);

    if (!guard.success()) {
        for (var step : guard.unmatched()) {
            if (!step.isCompleted()) step.cancel("Partial failure");
        }
        ctx.fail(new RuntimeException("Not all items could be reserved"));
    }
}
```

### Lifecycle listeners

```java
@Workflow(idProperty = "id", startOnEventClass = OrderPlaced.class,
          workflowNamespace = "de.eventmodelers.slices.{context}")
public class {SliceName}Workflow {

    public void execute(SimpleWorkflowContext ctx) { /* ... */ }

    @OnSuccess
    public void onSuccess(SimpleWorkflowContext ctx) {
        log.info("Workflow {} completed for id={}", ctx.workflowId(),
                 ctx.workflowPayload().get("id"));
    }

    @OnFailure
    public void onFailure(SimpleWorkflowContext ctx) {
        log.error("Workflow {} failed for id={}", ctx.workflowId(),
                  ctx.workflowPayload().get("id"));
    }

    @OnTimeout
    public void onTimeout(SimpleWorkflowContext ctx) {
        log.warn("Workflow {} timed out", ctx.workflowId());
    }
}
```

---

## Step 5: Feature Flag Configuration

Follow the same pattern as other slices:

`src/main/resources/application.properties`:
```properties
slices.{context}.automation.{slicename}.enabled=true
slices.{context}.workflows.enabled=true
```

`src/test/resources/application.properties`:
```properties
slices.{context}.automation.{slicename}.enabled=false
slices.{context}.workflows.enabled=false
```

`META-INF/additional-spring-configuration-metadata.json` — add entries for the new properties.

See [references/feature-flag-patterns.md](references/feature-flag-patterns.md) for the full pattern.

---

## Step 6: Tests

### Unit tests — test `execute()` logic with mock services

For simple workflows, stub the service methods and verify step sequencing:

```java
class {SliceName}WorkflowTest {

    private {SliceName}Workflow workflow;
    private {SliceName}WorkflowServices services;
    private SimpleWorkflowContext ctx;

    @BeforeEach
    void setUp() {
        services = mock({SliceName}WorkflowServices.class);
        workflow  = new {SliceName}Workflow(services);
        ctx       = mock(SimpleWorkflowContext.class);
        // stub workflowId() and workflowPayload() as needed
        when(ctx.workflowId()).thenReturn("wf-1");
        when(ctx.workflowPayload()).thenReturn(Map.of("id", "entity-1"));
    }

    @Test
    @DisplayName("given trigger event, workflow executes all steps in order")
    void happyPath() {
        // stub ctx.awaitExecute to return normally
        doNothing().when(ctx).awaitExecute(anyString(), anyMap(), any());
        var mockEvent = mock(WorkflowStepResult.class);
        when(ctx.waitForEvent(anyString(), any(), any())).thenReturn(mockEvent);
        when(mockEvent.timeout()).thenReturn(false);

        workflow.execute(ctx);

        // verify steps were called in order
        var inOrder = inOrder(ctx);
        inOrder.verify(ctx).awaitExecute(eq("step1-{commandName}"), anyMap(), any());
        inOrder.verify(ctx).waitForEvent(eq("step2-await{EventName}"), any(), any());
        inOrder.verify(ctx).awaitExecute(eq("step3-{secondCommandName}"), anyMap(), any());
    }

    @Test
    @DisplayName("given step2 times out, workflow fails")
    void timeoutFails() {
        doNothing().when(ctx).awaitExecute(anyString(), anyMap(), any());
        var timedOut = mock(WorkflowStepResult.class);
        when(ctx.waitForEvent(anyString(), any(), any())).thenReturn(timedOut);
        when(timedOut.timeout()).thenReturn(true);

        workflow.execute(ctx);

        verify(ctx).fail(any(RuntimeException.class));
    }

    @Test
    @DisplayName("given step2 payment fails, step1 stock is released")
    void compensatesOnPaymentFailure() {
        doNothing().when(ctx).awaitExecute(eq("reserveStock"), anyMap(), any());
        doThrow(new StepFailedException("insufficient funds"))
            .when(ctx).awaitExecute(eq("chargePayment"), anyMap(), any());

        workflow.execute(ctx);

        var inOrder = inOrder(ctx);
        inOrder.verify(ctx).awaitExecute(eq("chargePayment"), anyMap(), any());
        inOrder.verify(ctx).awaitExecute(eq("releaseStock"), anyMap(), any());
        inOrder.verify(ctx).fail(any());
    }
}
```

### Integration tests — `AbstractDeclarativeTestBase`

For full integration tests, use AF5's `AbstractDeclarativeTestBase` (requires the workflow module
on the test classpath):

```java
@SpringBootTest
@TestPropertySource(properties = {
    "slices.{context}.workflows.enabled=true",
    "slices.{context}.automation.{slicename}.enabled=true"
})
class {SliceName}WorkflowIntegrationTest extends AbstractDeclarativeTestBase {

    // AbstractDeclarativeTestBase provides fixture methods for
    // publishing events and asserting workflow state/steps.
    // Refer to the Axon Framework test documentation for the full API.
}
```

---

## Files to Create / Modify

```
src/main/java/de/eventmodelers/slices/{context}/automation/{slicename}/
├── {SliceName}Workflow.java              ← @Workflow class (execute method + lifecycle listeners)
└── {SliceName}WorkflowServices.java      ← @Component step handlers (Spring beans)

src/main/java/de/eventmodelers/slices/{context}/
└── WorkflowConfiguration.java            ← WorkflowModule bean (one per context, create if missing)

src/test/java/de/eventmodelers/slices/{context}/automation/{slicename}/
└── {SliceName}WorkflowTest.java          ← unit tests with mocked context + services
```

---

## References

- [Axon Workflow API](references/axon-workflow-api.md) — Annotations, context API, configuration, patterns
- [Feature Flag Patterns](references/feature-flag-patterns.md) — `@ConditionalOnProperty` and alternatives
- [Axon Workflow documentation](https://docs.axoniq.io/axon-framework-reference/development/workflows/) — Official reference (Preview)
