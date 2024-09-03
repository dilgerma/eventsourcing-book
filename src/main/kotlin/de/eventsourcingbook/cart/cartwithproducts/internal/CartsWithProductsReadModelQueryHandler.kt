package de.eventsourcingbook.cart.cartwithproducts.internal

import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModel
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class CartsWithProductsReadModelQueryHandler(
    private val repository: CartsWithProductsReadModelRepository
) {

    @QueryHandler
    fun handleQuery(query: CartsWithProductsReadModelQuery): CartsWithProductsReadModel? {
        return CartsWithProductsReadModel(repository.findByProductId(query.productId))
    }
}
