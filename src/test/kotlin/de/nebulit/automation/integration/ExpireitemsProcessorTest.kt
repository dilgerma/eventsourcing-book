package de.nebulit.automation.integration

import de.nebulit.common.support.BaseIntegrationTest
import de.nebulit.common.support.RandomData
import de.nebulit.common.support.StreamAssertions
import de.nebulit.common.support.awaitUntilAssserted
import de.nebulit.domain.commands.command.AddTodoCommand
import de.nebulit.events.TodoExpiredEvent
import java.time.LocalDateTime
import java.util.*
import org.axonframework.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**  */
class ExpireitemsProcessorTest : BaseIntegrationTest() {
  @Autowired private lateinit var commandGateway: CommandGateway

  @Autowired private lateinit var streamAssertions: StreamAssertions

  @Test
  fun `Expireitems Processor Test`() {
    val aggregateId = UUID.randomUUID()

    var addTodoCommand =
        RandomData.newInstance<AddTodoCommand> {
          this.aggregateId = aggregateId
          this.expirationDate = LocalDateTime.now().minusWeeks(1)
        }

    var addTodoCommandResult = commandGateway.sendAndWait<Any>(addTodoCommand)

    awaitUntilAssserted {
      streamAssertions.assertEvent(aggregateId.toString()) { it is TodoExpiredEvent }
    }
  }
}
