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
class CartItemsRessource(
    private var queryGateway: QueryGateway,
) {
    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @GetMapping("/{cartId}/cartitems")
    fun findReadModel(
        @PathVariable cartId: UUID,
    ): CompletableFuture<CartItemsReadModel> = queryGateway.query(CartItemsReadModelQuery(cartId), CartItemsReadModel::class.java)
}
