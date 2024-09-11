package de.nebulit.readmodell.internal

import de.nebulit.common.Event
import de.nebulit.common.QueryHandler
import de.nebulit.readmodell.TodosReadModel
import de.nebulit.readmodell.TodosReadModelQuery
import org.axonframework.eventsourcing.eventstore.EventStore
import org.springframework.stereotype.Component

@Component
class TodosReadModelQueryHandler(val eventStore: EventStore) :
    QueryHandler<TodosReadModelQuery, TodosReadModel> {

  @org.axonframework.queryhandling.QueryHandler
  override fun handleQuery(query: TodosReadModelQuery): TodosReadModel {
    val events =
        eventStore
            .readEvents(query.aggregateId.toString())
            .asStream()
            .filter { it.payload is Event }
            .map { it.payload as Event }
            .toList()

    return TodosReadModel().applyEvents(events)
  }
}
