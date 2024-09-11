package de.nebulit.readmodell

import de.nebulit.common.Event
import de.nebulit.common.Query
import de.nebulit.common.ReadModel
import de.nebulit.events.TodoAddedEvent
import java.util.UUID

class TodosReadModelQuery(
    var aggregateId: UUID,
) : Query

class TodosReadModel : ReadModel {
  var aggregateId: UUID? = null
  var todo: String? = null

  fun applyEvents(events: List<Event>): TodosReadModel {
    events.forEach { event ->
      when (event) {
        is TodoAddedEvent -> {
          aggregateId = event.aggregateId
          todo = event.todo
        }
      }
    }

    return this
  }
}
