# Axon Framework 5 — Workflow API Reference

> Preview feature — APIs may change. Source: https://docs.axoniq.io/axon-framework-reference/development/workflows/

---

## `@Workflow` annotation

```java
@Workflow(
    idProperty        = "orderId",               // field on trigger event used as workflow instance ID
    startOnEventClass = OrderPlacedEvent.class,  // event that starts a new instance
    workflowNamespace = "io.myapp.orders"        // namespace for internal event names
)
public void execute(SimpleWorkflowContext ctx) { ... }
```

Alternative: start by event name string (useful for sub-workflows started via `EventSink`):

```java
@Workflow(
    idProperty       = "childWorkflowId",
    startOnEventName = "io.myapp.StartPaymentProcess",
    workflowNamespace = "io.myapp.payments"
)
public void execute(SimpleWorkflowContext ctx) { ... }
```

---

## `SimpleWorkflowContext` — core API

### Data access

```java
ctx.workflowId()                 // String — the workflow instance ID
ctx.workflowPayload()            // Map<String,Object> — durable payload (snapshot of trigger event + updates)
```

### Execution steps

```java
// Blocking: dispatches action and waits for completion. Throws StepFailedException on failure.
ctx.awaitExecute(
    stepName,                    // String — unique name within this workflow instance
    payload,                     // Map<String,Object> — data passed to the handler
    handler                      // BiFunction<ProcessingContext, Map<String,Object>, Map<String,Object>>
);

// Non-blocking: dispatches action, returns handle immediately
WorkflowStepResult result = ctx.execute(
    stepName,
    payload,
    handler,
    Duration.ofSeconds(30),      // step timeout
    defaults()                   // WorkflowStepOptions
);
```

### Event suspension

```java
// Wait until event arrives or timeout fires. Returns WorkflowStepResult.
WorkflowStepResult r = ctx.waitForEvent(
    stepName,
    SomeEvent.class,
    Duration.ofDays(3)
);
r.timeout()          // true if timeout fired before event arrived
r.await()            // blocks until completed (call after !timeout() check)
r.result()           // Optional<T> — the event payload

// With association predicate (match specific event by payload field)
ctx.awaitEvent(
    stepName,
    SomeEvent.class,
    associate(payloadProperty("orderId"), equalsTo(ctx.workflowId())),
    Duration.ofMinutes(30)
);
```

### Timer

```java
// Durable sleep — survives restarts, recalculates remaining delay on resume
ctx.sleep(stepName, Duration.ofHours(24));
```

### Orchestration combinators

```java
// Fan-in: wait until ALL matching. Returns a guard with matched/unmatched.
var guard = ctx.allMatch(WorkflowStepResult::isCompleted, step1, step2, step3);
guard.success()      // true if all matched
guard.matched()      // list of steps that matched
guard.unmatched()    // list of steps that did not match

// Scatter-gather: wait until ANY matching. Returns the first matching result.
var fastest = ctx.anyMatch(WorkflowStepResult::isCompleted, step1, step2, step3);
fastest.await()
fastest.matched().getFirst()   // the winning step
```

### State mutation

```java
// Persist new data to the durable payload (survives restarts)
ctx.setPayload(stepName, payload("count", newCount).getValues());
```

### Termination

```java
ctx.fail(new RuntimeException("reason"))   // FAILED terminal state
ctx.cancel()                               // CANCELLED terminal state
```

---

## Workflow states

| State | Trigger |
|---|---|
| `STARTED` | Workflow instance created |
| `COMPLETED` | `execute()` method returned normally |
| `FAILED` | `ctx.fail()` called, or unhandled exception |
| `CANCELLED` | `ctx.cancel()` called, or external cancellation |
| `TIMED_OUT` | Overall workflow timeout exceeded |

---

## Lifecycle listeners

```java
@OnSuccess   public void onSuccess(SimpleWorkflowContext ctx)  { ... }
@OnFailure   public void onFailure(SimpleWorkflowContext ctx)  { ... }
@OnCancellation public void onCancelled(SimpleWorkflowContext ctx) { ... }
@OnTimeout   public void onTimeout(SimpleWorkflowContext ctx)  { ... }
```

Or programmatically via `.registerWorkflowStatusChangeListener(WorkflowStatus.COMPLETED, (status, ctx) -> {...})`.

---

## `WorkflowModule` configuration

```java
WorkflowModule
    .usingContext(SimpleWorkflowContext.class)
    .workflowContextFactory(c -> new SimpleWorkflowContextFactory())
    .workflowExecutionFactory(c -> new DSLAdoptingExecutionFactory<>(SimpleWorkflowContext.class))
    .definitions(d -> d
        // Annotation-based (reads @Workflow from the instance)
        .autodetected(c -> new MyWorkflow(), SimpleWorkflowContext.class)

        // Declarative (explicit name + trigger)
        .declarative(c -> workflow::execute)
        .workflowName("OrderFulfillment")
        .on(EventConditions.fromType(OrderPlacedEvent.class))
        .customized((c, w) -> w
            .eventNameCustomizer(namespace("io.myapp.orders"))
            .workflowIdProvider(fromPayloadAttribute(c, "orderId"))
            .registerWorkflowStatusChangeListener(WorkflowStatus.COMPLETED,
                (status, ctx) -> logger.info("Done: {}", ctx.workflowId()))
        )
    );
```

### Start condition with predicate

```java
.on(EventConditions.fromType(
    OrderPlacedEvent.class,
    associate(payloadProperty("type"), equalsTo("priority"))  // only start for priority orders
))
```

### Workflow ID providers

```java
fromPayloadAttribute(c, "orderId")                  // extract "orderId" field as-is
fromPayloadAttribute(c, "orderId", id -> "o-" + id) // with transformation
// default: MessageWorkflowIdProvider — uses the event message's native ID
```

### Custom context (domain-specific DSL)

```java
public class ApprovalWorkflowContext extends SimpleWorkflowContext {
    public ApprovalWorkflowContext(String workflowId, Map<String, Object> payload,
                                   ProcessingContext pc, WorkflowConfiguration<?> cfg) {
        super(workflowId, payload, pc, cfg);
    }

    public boolean requestApproval(String approver, Duration deadline) {
        awaitExecute("requestApproval",
            payload("approver", approver, "requestId", workflowId()).getValues(),
            ApprovalService::sendRequest);
        var decision = awaitEvent("awaitDecision", ApprovalDecisionEvent.class, deadline);
        return "approved".equals(decision.result().map(ApprovalDecisionEvent::outcome).orElse(""));
    }
}

// Factory
public class ApprovalContextFactory implements WorkflowContextFactory<ApprovalWorkflowContext> {
    @Override
    public ApprovalWorkflowContext createContext(Map<String, Object> payload, String workflowId,
                                                 ProcessingContext pc, WorkflowConfiguration<?> cfg) {
        return new ApprovalWorkflowContext(workflowId, payload, pc, cfg);
    }
}
```

---

## Metadata keys propagated by the engine

| Key | Value |
|---|---|
| `workflowId` | Workflow instance ID |
| `stepName` | Name of the currently executing step |
| `stepType` | Type of step (execute, event, sleep, etc.) |
| `workflowStatus` | Current workflow status |

---

## Sub-workflow pattern

```java
// Parent: publish trigger event for child via EventSink (no workflow metadata)
ctx.awaitExecute("startChild",
    payload("childId", "child-" + ctx.workflowId(), "orderId", ctx.workflowId()).getValues(),
    (pc, p) -> {
        var trigger = new StartChildWorkflow((String) p.get("childId"), (String) p.get("orderId"));
        eventSink.publish(null, new GenericEventMessage<>(
            messageTypeResolver.resolveOrThrow(trigger), trigger));
        return Map.of();
    });

// Parent: wait for child completion event, matched by orderId
ctx.awaitEvent("awaitChild",
    ChildWorkflowCompleted.class,
    associate(payloadProperty("orderId"), equalsTo(ctx.workflowId())),
    Duration.ofMinutes(30));
```

Child workflow:
```java
@Workflow(idProperty = "childId",
          startOnEventName = "io.myapp.StartChildWorkflow",
          workflowNamespace = "io.myapp.child")
public void execute(SimpleWorkflowContext ctx) {
    // ... do work ...
    // Signal parent via EventSink
    ctx.awaitExecute("notifyParent", ctx.workflowPayload(),
        (pc, p) -> {
            var done = new ChildWorkflowCompleted(
                (String) p.get("orderId"), (String) p.get("childId"));
            eventSink.publish(null, new GenericEventMessage<>(
                messageTypeResolver.resolveOrThrow(done), done));
            return Map.of();
        });
}
```

---

## `payload(...)` helper

```java
payload("key1", value1, "key2", value2).getValues()   // returns Map<String, Object>
```

---

## Testing

Use `AbstractDeclarativeTestBase` from the AF5 workflow test module for integration tests.
For unit tests, mock `SimpleWorkflowContext` with Mockito and verify step call order.