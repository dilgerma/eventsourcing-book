package de.eventsourcingbook.cart.events.versioned

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

@Deprecated("V2")
data class ItemAddedEvent(
    var aggregateId: UUID,
    var description: String,
    var image: String,
    var price: Double,
    var itemId: UUID,
    var productId: UUID,
) : Event
