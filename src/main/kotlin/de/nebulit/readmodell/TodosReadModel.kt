package de.nebulit.readmodell

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.sql.Types
import java.time.LocalDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode

class ExpiredTodoItemsQuery

@Entity
class TodosReadModelEntity {
  @Id @GeneratedValue val id: Long? = null

  @JdbcTypeCode(Types.VARCHAR) @Column(name = "aggregateId") lateinit var aggregateId: UUID

  @Column(name = "todo") lateinit var todo: String

  @Column(name = "expiration_date") lateinit var expirationDate: LocalDateTime

  @Column(name = "expired") var expired: Boolean = false
}

data class ExpiredTodoData(
    val aggregateId: UUID,
    val todo: String,
)

data class TodosReadModel(
    val data: List<ExpiredTodoData>,
)
