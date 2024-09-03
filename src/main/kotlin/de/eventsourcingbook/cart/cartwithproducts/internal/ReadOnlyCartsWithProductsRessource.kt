package de.eventsourcingbook.cart.cartwithproducts.internal

import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModel
import de.eventsourcingbook.cart.cartwithproducts.CartsWithProductsReadModelQuery
import java.util.UUID
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging
import org.axonframework.queryhandling.QueryGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class CartwithproductsRessource(private var queryGateway: QueryGateway) {

    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @GetMapping("/cartwithproducts/{productId}")
    fun findReadModel(
        @PathVariable("productId") productId: UUID
    ): CompletableFuture<CartsWithProductsReadModel> {
        return queryGateway.query(
            CartsWithProductsReadModelQuery(productId),
            CartsWithProductsReadModel::class.java
        )
    }
}
