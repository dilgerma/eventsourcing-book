package de.eventsourcingbook.cart.cartwithproducts.internal

import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModel
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelEntity
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelQuery
import de.eventsourcingbook.cart.events.ItemAddedEvent
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedDeque

@ProcessingGroup("cart-with-products")
@Component
class CartsWithProductsReadModelQueryHandler(
    private val repository: CartsWithProductsReadModelRepository,
) {
    val limitedDeque = ConcurrentLinkedDeque<CartsWithProductsReadModelEntity>()

    @QueryHandler
    fun handleQuery(query: CartsWithProductsReadModelQuery): CartsWithProductsReadModel? {
        val data = repository.findByProductId(query.productId)
        val set = data.toMutableSet()
        set.addAll(limitedDeque)
        return CartsWithProductsReadModel(set.toList())
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
