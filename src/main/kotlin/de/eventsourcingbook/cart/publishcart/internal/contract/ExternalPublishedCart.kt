package de.eventsourcingbook.cart.publishcart.internal.contract

import java.util.UUID

data class OrderedProduct(
    val productId: UUID,
    val price: Double,
)

data class ExternalPublishedCart(
    val aggregateId: UUID,
    val totalPrice: Double,
    val orderedProducts: List<OrderedProduct>,
)
