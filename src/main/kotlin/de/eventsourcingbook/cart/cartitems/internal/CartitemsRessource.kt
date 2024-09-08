package de.eventsourcingbook.cart.cartitems.internal

import de.eventsourcingbook.cart.cartitems.CartItemsReadModel
import de.eventsourcingbook.cart.cartitems.CartItemsReadModelQuery
import mu.KotlinLogging
import org.axonframework.queryhandling.QueryGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.CompletableFuture

@RestController
class CartitemsRessource(
    private var queryGateway: QueryGateway,
) {
    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @GetMapping("/cartitems/{aggregateId}")
    fun findReadModel(
        @PathVariable("aggregateId") aggregateId: UUID,
    ): CompletableFuture<CartItemsReadModel> = queryGateway.query(CartItemsReadModelQuery(aggregateId), CartItemsReadModel::class.java)
}
