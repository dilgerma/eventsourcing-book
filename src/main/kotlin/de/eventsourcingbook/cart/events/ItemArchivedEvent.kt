package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

data class ItemArchivedEvent(var aggregateId: UUID, var itemId: UUID) : Event
