package de.eventsourcingbook.cart.cartwithproducts.internal

import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModel
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelQuery
import mu.KotlinLogging
import org.axonframework.queryhandling.QueryGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.CompletableFuture

@RestController
class CartsWithProductsRessource(
    private var queryGateway: QueryGateway,
) {
    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @GetMapping("/cartwithproducts/{productId}")
    fun findReadModel(
        @PathVariable("productId") productId: UUID,
    ): CompletableFuture<CartsWithProductsReadModel> =
        queryGateway.query(
            CartsWithProductsReadModelQuery(productId),
            CartsWithProductsReadModel::class.java,
        )
}
