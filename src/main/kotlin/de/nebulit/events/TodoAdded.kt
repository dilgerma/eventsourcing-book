package de.nebulit.events

import de.nebulit.common.Event
import java.util.UUID

data class TodoAdded(
    var aggregateId: UUID,
    var todo: String,
) : Event
