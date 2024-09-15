package de.nebulit.readmodell.internal

import de.nebulit.readmodell.ExpiredTodoData
import de.nebulit.readmodell.ExpiredTodoItemsQuery
import de.nebulit.readmodell.TodosReadModel
import java.time.LocalDateTime
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class TodosReadModelQueryHandler(
    private val repository: TodosReadModelRepository,
) {
  @QueryHandler
  fun handleQuery(query: ExpiredTodoItemsQuery): TodosReadModel? {
    var todos = repository.findByExpiredFalseAndExpirationDateBefore(LocalDateTime.now())
    return TodosReadModel(
        todos.map { ExpiredTodoData(it.aggregateId, it.todo) },
    )
  }
}
