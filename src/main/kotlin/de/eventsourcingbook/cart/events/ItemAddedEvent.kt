package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

data class ItemAddedEvent(
    var aggregateId: UUID,
    var description: String,
    var image: String,
    var price: Double,
    var itemId: UUID,
    var productId: UUID,
) : Event
