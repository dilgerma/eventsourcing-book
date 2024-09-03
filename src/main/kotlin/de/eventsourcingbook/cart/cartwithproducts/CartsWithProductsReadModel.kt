package de.eventsourcingbook.cart.cartwithproducts

import de.eventsourcingbook.cart.common.NoArg
import jakarta.persistence.*
import java.util.UUID

class CartsWithProductsReadModelQuery(val productId: UUID)

@NoArg
data class CartProduct(
    @Id @Column(name = "aggregateId") val aggregateId: UUID,
    @Id @Column(name = "productId") val productId: UUID
) {}

@IdClass(CartProduct::class)
@Entity
class CartsWithProductsReadModelEntity {

    @Column(name = "aggregateId") @Id lateinit var aggregateId: UUID
    @Column(name = "productId") @Id lateinit var productId: UUID
}

data class CartsWithProductsReadModel(val data: List<CartsWithProductsReadModelEntity>)
