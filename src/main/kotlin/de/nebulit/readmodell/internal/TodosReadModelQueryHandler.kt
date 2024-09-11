package de.nebulit.readmodell.internal

import de.nebulit.events.TodoAddedEvent
import de.nebulit.readmodell.TodosReadModel
import de.nebulit.readmodell.TodosReadModelEntity
import de.nebulit.readmodell.TodosReadModelQuery
import java.util.concurrent.LinkedBlockingQueue
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@ProcessingGroup("partial-consistent")
@Component
class TodosReadModelQueryHandler(
    private val repository: TodosReadModelRepository,
) {
  val queue = LinkedBlockingQueue<TodosReadModelEntity>(20)

  @QueryHandler
  fun handleQuery(query: TodosReadModelQuery): TodosReadModel? {
    var result = queue.find { it.aggregateId == query.aggregateId }
    if (result == null) {
      result = repository.findById(query.aggregateId).orElse(null)
    }
    return result?.let { TodosReadModel(result) }
  }

  @EventHandler
  fun handle(event: TodoAddedEvent) {
    queue.add(
        TodosReadModelEntity().apply {
          this.todo = event.todo
          this.aggregateId = event.aggregateId
        },
    )
  }
}
