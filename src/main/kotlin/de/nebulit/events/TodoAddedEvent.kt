package de.nebulit.events

import de.nebulit.common.Event
import java.time.LocalDateTime
import java.util.UUID

data class TodoAddedEvent(
    var aggregateId: UUID,
    var todo: String,
    val expirationDate: LocalDateTime,
) : Event
