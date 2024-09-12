package de.nebulit.readmodell.internal

import de.nebulit.events.TodoAddedEvent
import de.nebulit.readmodell.TodosReadModelEntity
import java.util.UUID
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface TodosReadModelRepository : JpaRepository<TodosReadModelEntity, UUID> {
  fun findByAggregateId(aggregateId: UUID): List<TodosReadModelEntity>
}

@Component
class TodosReadModelProjector(
    var repository: TodosReadModelRepository,
) {
  @EventHandler
  fun on(event: TodoAddedEvent) {
    // throws exception if not available (adjust logic)
    val entity = TodosReadModelEntity()
    entity
        .apply {
          aggregateId = event.aggregateId
          todo = event.todo
        }
        .also { this.repository.save(it) }
  }
}
