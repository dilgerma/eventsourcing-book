package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

data class ItemRemovedEvent(var aggregateId: UUID, var itemId: UUID) : Event
