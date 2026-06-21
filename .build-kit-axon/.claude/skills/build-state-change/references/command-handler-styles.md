# Command Handler Styles

Axon Framework 5 supports multiple ways to register command handlers. Check the target project's
existing slices to determine which style is established, or ask the user.
Examples use a generic `Ordering` bounded context.

---

## Style 1: Separate `@Component` class (Spring Boot default)

The most common style for Spring Boot projects. The entity is `@EventSourced`, the handler is a
separate `@Component` class.

```java
// Entity — auto-discovered by Spring Boot component scanning
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@EventSourced(tagKey = EventTags.ORDER_ID)
class PlaceOrderEntity {

    private PlaceOrderState state;

    private PlaceOrderEntity(PlaceOrderState state) { this.state = state; }

    @EntityCreator
    public static PlaceOrderEntity create() {
        return new PlaceOrderEntity(PlaceOrderState.initial());
    }

    @EventSourcingHandler
    public PlaceOrderEntity on(OrderPlaced event) {
        return new PlaceOrderEntity(evolve(this.state, event));
    }

    PlaceOrderState state() { return state; }
}

// Handler — separate @Component; receives entity via @InjectEntity
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@Component
public class PlaceOrderHandler {

    @CommandHandler
    public void handle(
        PlaceOrderCommand command,
        AxonMetadata metadata,
        @InjectEntity PlaceOrderEntity entity,
        EventAppender eventAppender
    ) {
        var events = decide(command, entity.state());
        eventAppender.append(events, metadata);
    }
}
```

**Characteristics:**
- Handler and entity are separate classes
- Spring Boot auto-discovers both via component scanning
- `@ConditionalOnProperty` on **both** entity and handler (and REST controller)
- Tested with Spring Boot integration test (`@SpringBootTest` + `@Autowired AxonTestFixture`)

---

## Style 2: Explicit registration via `@Configuration`

Used when unit tests without Spring context are required. Entity and handler registered manually.

```java
// Entity — @EventSourcedEntity (no tagKey), uses @EventCriteriaBuilder
@EventSourcedEntity
class PlaceOrderEntity {

    private PlaceOrderState state;

    private PlaceOrderEntity(PlaceOrderState state) { this.state = state; }

    @EntityCreator
    public static PlaceOrderEntity create() {
        return new PlaceOrderEntity(PlaceOrderState.initial());
    }

    @EventSourcingHandler
    public PlaceOrderEntity on(OrderPlaced event) {
        return new PlaceOrderEntity(evolve(this.state, event));
    }

    @EventCriteriaBuilder
    public static EventCriteria resolveCriteria(OrderId orderId) {
        return EventCriteria
            .havingTags(Tag.of(EventTags.ORDER_ID, orderId.value()))
            .andBeingOneOfTypes("Ordering.OrderPlaced");
            // MUST use "Namespace.Name" strings — NEVER ClassName.class.getName()
    }

    PlaceOrderState state() { return state; }
}

// Handler — NO @Component; registered via Configuration below
class PlaceOrderHandler {

    @CommandHandler
    public void handle(
        PlaceOrderCommand command,
        AxonMetadata metadata,
        @InjectEntity PlaceOrderEntity entity,
        EventAppender eventAppender
    ) {
        var events = decide(command, entity.state());
        eventAppender.append(events, metadata);
    }
}

// Configuration — registers entity + handler explicitly
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@Configuration
class PlaceOrderWriteSliceConfig {

    @Bean
    public EntityModule<?, ?> placeOrderEntity() {
        return EventSourcedEntityModule.autodetected(
            OrderId.class,
            PlaceOrderEntity.class
        );
    }

    @Bean
    public CommandHandlingModule placeOrderSlice() {
        return CommandHandlingModule.named("PlaceOrder")
            .commandHandlers()
            .annotatedCommandHandlingComponent(PlaceOrderHandler::new)
            .build();
    }
}
```

**Characteristics:**
- No `@Component` on handler
- `@ConditionalOnProperty` on `@Configuration` class only (not on entity/handler)
- Unit-testable without Spring via `AxonTestFixture.configSlice(...)`

---

## Style 3: Multi-Tag DCB with Explicit Registration

Used when `decide()` needs state from events across **multiple** consistency boundaries (streams).
Same registration as Style 2 but with a composite ID and `EventCriteria.either(...)`.

```java
// Command with composite consistency boundary
@Command(namespace = "Ordering", name = "CheckoutCart", version = "1.0.0")
public record CheckoutCartCommand(
    String cartId,
    String customerId,
    List<String> items
) {}

// Composite ID — used by @EventCriteriaBuilder
record CheckoutCartId(String cartId, String customerId) {}

// Entity — criteria spans two streams
@EventSourcedEntity
class CheckoutCartEntity {

    @EntityCreator
    public static CheckoutCartEntity create() {
        return new CheckoutCartEntity(CheckoutCartState.initial());
    }

    @EventSourcingHandler
    public CheckoutCartEntity on(CartCreated event) {
        return new CheckoutCartEntity(evolve(this.state, event));
    }

    @EventSourcingHandler
    public CheckoutCartEntity on(CreditReserved event) {
        return new CheckoutCartEntity(evolve(this.state, event));
    }

    @EventCriteriaBuilder
    public static EventCriteria resolveCriteria(CheckoutCartId id) {
        return EventCriteria.either(
            EventCriteria
                .havingTags(Tag.of(EventTags.CART_ID, id.cartId()))
                .andBeingOneOfTypes("Ordering.CartCreated"),
            EventCriteria
                .havingTags(Tag.of(EventTags.CUSTOMER_ID, id.customerId()))
                .andBeingOneOfTypes("Customers.CreditReserved")
        );
    }

    // ... state, constructor, state()
}
```

**Key rule for `.andBeingOneOfTypes(...)`**: Use `"Namespace.Name"` string literals
(e.g., `"Ordering.CartCreated"`). This is the `@Event(namespace)` + `"."` + `@Event(name)` value.
**NEVER** use `ClassName.class.getName()`.

---

## Which style to choose?

| | Style 1 (`@Component`) | Style 2 (Explicit `@Configuration`) | Style 3 (Multi-Tag DCB) |
|---|---|---|---|
| Spring Boot project | ✅ Default | ✅ Also valid | ✅ Also valid |
| Non-Spring unit tests wanted | ❌ Needs Spring context | ✅ Works standalone | ✅ Works standalone |
| Single consistency boundary | ✅ | ✅ | — |
| Multiple consistency boundaries (DCB) | ✅ with `@EventSourcedEntity` + `@EventCriteriaBuilder` + `@Component` | ✅ | ✅ |