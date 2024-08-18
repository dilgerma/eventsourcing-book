package de.eventsourcingbook.cart.cartwithproducts.internal

import de.eventsourcingbook.cart.cartwithproducts.CartProduct
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelEntity
import de.eventsourcingbook.cart.events.CartClearedEvent
import de.eventsourcingbook.cart.events.ItemAddedEvent
import de.eventsourcingbook.cart.events.ItemArchivedEvent
import de.eventsourcingbook.cart.events.ItemRemovedEvent
import java.util.UUID
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Component

interface CartsWithProductsReadModelRepository :
    JpaRepository<CartsWithProductsReadModelEntity, CartProduct> {

  fun findByProductId(productId: UUID): List<CartsWithProductsReadModelEntity>

  @Modifying fun deleteAllByAggregateId(cartId: UUID): List<CartsWithProductsReadModelEntity>
}

@Component
class CartsWithProductsReadModelProjector(var repository: CartsWithProductsReadModelRepository) {

  @EventHandler
  fun on(event: CartClearedEvent) {
    // throws exception if not available (adjust logic)
    this.repository.deleteAllByAggregateId(event.aggregateId)
  }

  @EventHandler
  fun on(event: ItemRemovedEvent) {
    // throws exception if not available (adjust logic)
    this.repository.deleteById(CartProduct(event.aggregateId, event.itemId))
  }

  @EventHandler
  fun on(event: ItemArchivedEvent) {
    // throws exception if not available (adjust logic)
    this.repository.deleteById(CartProduct(event.aggregateId, event.itemId))
  }

  @EventHandler
  fun on(event: ItemAddedEvent) {
    // throws exception if not available (adjust logic)
    repository.save(
        CartsWithProductsReadModelEntity().apply {
          this.productId = event.productId
          this.aggregateId = event.aggregateId
        })
  }
}
