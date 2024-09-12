package de.nebulit.readmodell.internal

import de.nebulit.readmodell.TodoData
import de.nebulit.readmodell.TodosReadModel
import de.nebulit.readmodell.TodosReadModelEntity
import de.nebulit.readmodell.TodosReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class TodosReadModelQueryHandler(
    private val repository: TodosReadModelRepository,
) {
  @QueryHandler
  fun handleQuery(query: TodosReadModelQuery): TodosReadModel? {
    var todos = repository.findByAggregateId(query.aggregateId)
    return TodosReadModel(
        todos.map { TodoData(it.todo) }, query.aggregateId, calculateNumberOfTodos(todos))
  }

  private fun calculateNumberOfTodos(todos: List<TodosReadModelEntity>): Int = todos.size
}
