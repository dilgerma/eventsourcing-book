package de.eventsourcingbook.cart.cartwithproducts.internal

import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModel
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelEntity
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelQuery
import de.eventsourcingbook.cart.events.ItemAddedEvent
import java.util.concurrent.ConcurrentLinkedDeque
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@ProcessingGroup("cart-with-products")
@Component
class CartsWithProductsReadModelQueryHandler(
    private val repository: CartsWithProductsReadModelRepository,
) {
  val limitedDeque = ConcurrentLinkedDeque<CartsWithProductsReadModelEntity>()

  @QueryHandler
  fun handleQuery(query: CartsWithProductsReadModelQuery): CartsWithProductsReadModel? {
    var data = repository.findByProductId(query.productId)
    var list = data.toMutableList()
    list.addAll(limitedDeque)
    return CartsWithProductsReadModel(list)
  }

  @EventHandler
  fun on(event: ItemAddedEvent) {
    while (limitedDeque.size > 20) {
      limitedDeque.pollFirst()
    }
    var item =
        CartsWithProductsReadModelEntity().apply {
          this.productId = event.productId
          this.aggregateId = event.aggregateId
        }
    if (!limitedDeque.contains(item)) {
      limitedDeque.push(
          item,
      )
    }
  }
}
