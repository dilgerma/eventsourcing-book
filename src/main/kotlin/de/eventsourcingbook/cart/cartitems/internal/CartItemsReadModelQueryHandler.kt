package de.eventsourcingbook.cart.cartitems.internal

import de.eventsourcingbook.cart.cartitems.CartItemsReadModel
import de.eventsourcingbook.cart.cartitems.CartItemsReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class CartItemsReadModelQueryHandler(
    private val repository: CartItemsReadModelRepository,
) {
    @QueryHandler
    fun handleQuery(query: CartItemsReadModelQuery): CartItemsReadModel? =
        CartItemsReadModel(repository.findByAggregateId(query.aggregateId))
}
