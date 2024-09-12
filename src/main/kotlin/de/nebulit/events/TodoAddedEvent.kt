package de.nebulit.events

import de.nebulit.common.Event
import java.util.UUID

data class TodoAddedEvent(var aggregateId: UUID, var todo: String) : Event
