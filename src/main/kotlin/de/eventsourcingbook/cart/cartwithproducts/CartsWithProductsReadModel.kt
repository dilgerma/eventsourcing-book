package de.eventsourcingbook.cart.cartwithproducts

import de.eventsourcingbook.cart.common.NoArg
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.util.UUID

class CartsWithProductsReadModelQuery(
    val productId: UUID,
)

@NoArg
data class CartProduct(
    @Id @Column(name = "aggregateId") val aggregateId: UUID,
    @Id @Column(name = "productId") val productId: UUID,
)

@IdClass(CartProduct::class)
@Entity
class CartsWithProductsReadModelEntity {
    @Column(name = "aggregateId")
    @Id
    lateinit var aggregateId: UUID

    @Column(name = "productId")
    @Id
    lateinit var productId: UUID

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CartsWithProductsReadModelEntity) return false

        return aggregateId == other.aggregateId && productId == other.productId
    }

    override fun hashCode(): Int {
        var result = productId.hashCode()
        result = 31 * result + aggregateId.hashCode()
        return result
    }
}

data class CartsWithProductsReadModel(
    val data: List<CartsWithProductsReadModelEntity>,
)
