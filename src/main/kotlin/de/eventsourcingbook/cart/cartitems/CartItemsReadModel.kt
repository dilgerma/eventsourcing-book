package de.eventsourcingbook.cart.cartitems

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types
import java.util.UUID

data class CartItemsReadModelQuery(
    val aggregateId: UUID,
)

@Entity
class CartItemsReadModelEntity {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "itemId")
    var itemId: UUID? = null

    @Column(name = "aggregateId")
    var aggregateId: UUID? = null

    @Column(name = "description")
    var description: String? = null

    @Column(name = "image")
    var image: String? = null

    @Column(name = "price")
    var price: Double? = null

    @Column(name = "totalPrice")
    var totalPrice: Double? = null

    @Column(name = "productId")
    var productId: UUID? = null

    @Column(name = "fingerPrint")
    var fingerPrint: String? = null
}

data class CartItemsReadModel(
    val data: List<CartItemsReadModelEntity>,
)
