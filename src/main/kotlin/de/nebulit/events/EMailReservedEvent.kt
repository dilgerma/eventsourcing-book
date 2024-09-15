package de.nebulit.events

import de.nebulit.common.Event
import java.util.UUID

data class EMailReservedEvent(var aggregateId: UUID, var email: String) : Event
