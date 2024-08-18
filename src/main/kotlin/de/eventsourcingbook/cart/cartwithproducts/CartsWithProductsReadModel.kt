package de.eventsourcingbook.cart.cartwithproducts

import de.eventsourcingbook.cart.common.NoArg
import jakarta.persistence.*
import java.sql.Types
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode

class CartsWithProductsReadModelQuery(val productId: UUID)

@NoArg
data class CartProduct(
    @Id @Column(name = "aggregateId") val aggregateId: UUID,
    @Column(name = "productId") @Id val productId: UUID
) {}

@IdClass(CartProduct::class)
@Entity
class CartsWithProductsReadModelEntity {

  @Column(name = "aggregateId") @Id var aggregateId: UUID? = null
  @Column(name = "productId") var productId: UUID? = null
}

data class CartsWithProductsReadModel(val data: List<CartsWithProductsReadModelEntity>)
