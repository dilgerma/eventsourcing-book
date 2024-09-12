package de.nebulit.readmodell.integration

import de.nebulit.common.CommandResult
import de.nebulit.common.support.BaseIntegrationTest
import de.nebulit.common.support.RandomData
import de.nebulit.common.support.awaitUntilAssserted
import de.nebulit.domain.commands.command.AddTodoCommand
import de.nebulit.readmodell.TodosReadModel
import de.nebulit.readmodell.TodosReadModelQuery
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**  */
class ReadmodellReadModelTest : BaseIntegrationTest() {
  @Autowired private lateinit var commandGateway: CommandGateway

  @Autowired private lateinit var queryGateway: QueryGateway

  @Test
  fun `Readmodell Read Model Test`() {
    val aggregateId = UUID.randomUUID()

    commandGateway.sendAndWait<CommandResult>(
        RandomData.newInstance<AddTodoCommand> {
          this.aggregateId = aggregateId
          this.todo = "todo1"
        },
    )
    commandGateway.sendAndWait<CommandResult>(
        RandomData.newInstance<AddTodoCommand> {
          this.aggregateId = aggregateId
          this.todo = "todo2"
        },
    )

    awaitUntilAssserted {
      var readModel =
          queryGateway.query(TodosReadModelQuery(aggregateId), TodosReadModel::class.java)
      assertThat(readModel.get()).isNotNull
      assertThat(readModel.get().todoCount).isEqualTo(2)
    }
  }
}
