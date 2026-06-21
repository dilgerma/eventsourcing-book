# Feature Flag Patterns

How to conditionally enable/disable individual slice components. Examples use a generic `Ordering` bounded context.

---

## Option 1: Spring Boot `@ConditionalOnProperty` (default)

Add `@ConditionalOnProperty` to every conditionally-loaded component in the slice. Update config files
and Spring metadata so the IDE auto-completes property keys.

### Annotation on slice components

```java
// Entity
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@EventSourced(tagKey = EventTags.ORDER_ID)
class PlaceOrderEntity { ... }

// Handler
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@Component
public class PlaceOrderHandler { ... }

// REST controller (if applicable)
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@RestController
public class PlaceOrderRestController { ... }
```

For the **Explicit Registration pattern**, put `@ConditionalOnProperty` on the `@Configuration` class only:

```java
@ConditionalOnProperty(prefix = "slices.ordering", name = "write.placeorder.enabled")
@Configuration
class PlaceOrderWriteSliceConfig { ... }
```

### `application.properties` (main — enable by default)

```properties
slices.ordering.write.placeorder.enabled=true
slices.ordering.read.getorders.enabled=true
slices.ordering.automation.notifycustomeronorder.enabled=true
```

### `application.properties` (test — disable by default, opt-in per test)

```properties
slices.ordering.write.placeorder.enabled=false
slices.ordering.read.getorders.enabled=false
slices.ordering.automation.notifycustomeronorder.enabled=false
```

### Opt-in in a specific test class

```java
@TestPropertySource(properties = {"slices.ordering.write.placeorder.enabled=true"})
@SpringBootTest
class PlaceOrderTest { ... }
```

### `META-INF/additional-spring-configuration-metadata.json`

Register each property so IDEs auto-complete and validate it:

```json
{
  "properties": [
    {
      "name": "slices.ordering.write.placeorder.enabled",
      "type": "java.lang.Boolean",
      "description": "Enable/disable the PlaceOrder write slice in the Ordering bounded context."
    },
    {
      "name": "slices.ordering.read.getorders.enabled",
      "type": "java.lang.Boolean",
      "description": "Enable/disable the GetOrders read slice in the Ordering bounded context."
    }
  ]
}
```

---

## Option 2: Spring Profile (`@Profile`)

Enable/disable components by activating a named Spring profile. Simpler for "all or nothing" rollouts,
less granular than `@ConditionalOnProperty`.

```java
@Profile("ordering-write")
@Component
public class PlaceOrderHandler { ... }
```

Activate in tests via `@ActiveProfiles("ordering-write")` or `spring.profiles.active=ordering-write`.

---

## Option 3: No feature flags

If the project ships all slices unconditionally, omit `@ConditionalOnProperty` entirely. Applicable when:
- The project is small and startup time is not a concern
- There is no partial-rollout requirement
- Feature branches are the rollout mechanism

---

## Convention recommendation

- Use `@ConditionalOnProperty` for new projects: zero extra dependencies, IDE-friendly, test-friendly.
- Disable all slices by default in the test properties; opt-in per test class via `@TestPropertySource`.
- Always enable BOTH the slice under test AND its dependencies (e.g., the automation AND the target write slice).