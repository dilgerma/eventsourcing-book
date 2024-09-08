package de.eventsourcingbook.cart.cartitems.internal

import de.eventsourcingbook.cart.cartitems.CartItemsReadModelEntity
import de.eventsourcingbook.cart.events.ItemAddedEvent
import de.eventsourcingbook.cart.events.ItemArchivedEvent
import de.eventsourcingbook.cart.events.ItemRemovedEvent
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

interface CartItemsReadModelRepository : JpaRepository<CartItemsReadModelEntity, UUID> {
    fun findByAggregateId(aggregateId: UUID): List<CartItemsReadModelEntity>

    fun deleteByAggregateId(aggregateId: UUID)
}

@Component
class CartItemsReadModelProjector(
    var repository: CartItemsReadModelRepository,
) {
    @EventHandler
    fun on(event: ItemArchivedEvent) {
        this.repository.deleteById(event.aggregateId)
    }

    @EventHandler
    fun on(event: ItemRemovedEvent) {
        this.repository.deleteById(event.aggregateId)
    }

    @EventHandler
    fun on(event: ItemAddedEvent) {
        // throws exception if not available (adjust logic)
        val entity = this.repository.findById(event.aggregateId).orElse(CartItemsReadModelEntity())
        entity
            .apply {
                aggregateId = event.aggregateId
                description = event.description
                image = event.image
                price = event.price
                itemId = event.itemId
                totalPrice = event.price
                productId = event.productId
                fingerPrint = event.deviceFingerPrint
            }.also { this.repository.save(it) }
    }
}
