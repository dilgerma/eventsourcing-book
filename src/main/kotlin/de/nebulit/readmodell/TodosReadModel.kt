package de.nebulit.readmodell

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.sql.Types
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode

data class TodosReadModelQuery(
    val aggregateId: UUID,
)

@Entity
class TodosReadModelEntity {
  @Id @GeneratedValue val id: Long? = null

  @JdbcTypeCode(Types.VARCHAR) @Column(name = "aggregateId") var aggregateId: UUID? = null

  @Column(name = "todo") lateinit var todo: String
}

data class TodoData(
    val todo: String,
)

data class TodosReadModel(
    val data: List<TodoData>,
    val aggregateId: UUID,
    val todoCount: Int,
)
