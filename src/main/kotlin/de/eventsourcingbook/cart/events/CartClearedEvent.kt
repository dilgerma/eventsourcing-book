package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

data class CartClearedEvent(var aggregateId: UUID) : Event
