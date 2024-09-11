package de.nebulit.readmodell.internal

import de.nebulit.readmodell.TodosReadModel
import de.nebulit.readmodell.TodosReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class TodosReadModelQueryHandler(
    private val repository: TodosReadModelRepository,
) {
  @QueryHandler
  fun handleQuery(query: TodosReadModelQuery): TodosReadModel? {
    if (!repository.existsById(query.aggregateId)) {
      return null
    }
    return TodosReadModel(repository.findById(query.aggregateId).get())
  }
}
