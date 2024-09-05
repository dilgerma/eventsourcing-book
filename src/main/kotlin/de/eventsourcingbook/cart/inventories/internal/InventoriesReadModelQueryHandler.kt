package de.eventsourcingbook.cart.inventories.internal

import de.eventsourcingbook.cart.inventories.InventoriesReadModel
import de.eventsourcingbook.cart.inventories.InventoriesReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class InventoriesReadModelQueryHandler(private val repository: InventoriesReadModelRepository) {

    @QueryHandler
    fun handleQuery(query: InventoriesReadModelQuery): InventoriesReadModel? {
        if (!repository.existsById(query.productId)) {
            return null
        }
        return InventoriesReadModel(repository.findById(query.productId).get())
    }
}
