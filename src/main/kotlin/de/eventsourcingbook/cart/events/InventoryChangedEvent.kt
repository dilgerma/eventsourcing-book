package de.eventsourcingbook.cart.events

import de.eventsourcingbook.cart.common.Event
import java.util.UUID

data class InventoryChangedEvent(
    var productId: UUID,
    var inventory: Int,
) : Event
