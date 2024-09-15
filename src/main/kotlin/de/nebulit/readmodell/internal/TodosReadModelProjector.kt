package de.nebulit.readmodell.internal

import de.nebulit.events.TodoAddedEvent
import de.nebulit.events.TodoExpiredEvent
import de.nebulit.readmodell.TodosReadModelEntity
import java.time.LocalDateTime
import java.util.UUID
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface TodosReadModelRepository : JpaRepository<TodosReadModelEntity, UUID> {
  fun findByAggregateIdAndTodo(
      aggregateId: UUID,
      todo: String,
  ): TodosReadModelEntity?

  fun findByExpiredFalseAndExpirationDateBefore(now: LocalDateTime): List<TodosReadModelEntity>
}

@Component
class TodosReadModelProjector(
    var repository: TodosReadModelRepository,
) {
  @EventHandler
  fun on(event: TodoAddedEvent) {
    val entity = TodosReadModelEntity()
    entity
        .apply {
          aggregateId = event.aggregateId
          todo = event.todo
          expirationDate = event.expirationDate
        }
        .also { this.repository.save(it) }
  }

  @EventHandler
  fun on(event: TodoExpiredEvent) {
    val entity = repository.findByAggregateIdAndTodo(event.aggregateId, event.todo)
    entity?.let {
      it.expired = true
      this.repository.save(it)
    }
  }
}
