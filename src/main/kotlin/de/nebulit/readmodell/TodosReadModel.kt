package de.nebulit.readmodell

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.sql.Types
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode

data class TodosReadModelQuery(
    val aggregateId: UUID,
)

@Entity
class TodosReadModelEntity {
  @Id @JdbcTypeCode(Types.VARCHAR) @Column(name = "aggregateId") var aggregateId: UUID? = null

  @Column(name = "todo") var todo: String? = null
}

data class TodosReadModel(
    val data: TodosReadModelEntity,
)
