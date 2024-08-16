package de.eventsourcingbook.cart.inventories

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.sql.Types
import java.util.*
import org.hibernate.annotations.JdbcTypeCode

data class InventoriesReadModelQuery(val productId: UUID)

@Entity
class InventoriesReadModelEntity {
  @Id @JdbcTypeCode(Types.VARCHAR) @Column(name = "productId") var productId: UUID? = null

  @Column(name = "inventory") var inventory: Int? = null
}

data class InventoriesReadModel(val data: InventoriesReadModelEntity)
