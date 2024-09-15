package de.nebulit.automation.internal

import de.nebulit.common.Processor
import de.nebulit.domain.commands.automation.ExpireTodoCommand
import de.nebulit.readmodell.ExpiredTodoItemsQuery
import de.nebulit.readmodell.TodosReadModel
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AutomationProcessor : Processor {
  var logger = KotlinLogging.logger {}

  @Autowired lateinit var commandGateway: CommandGateway

  @Autowired lateinit var queryGateway: QueryGateway

  @Scheduled(fixedDelay = 1000L)
  fun handleExpirations() {
    // query all data
    queryGateway
        .query(
            ExpiredTodoItemsQuery(),
            TodosReadModel::class.java,
        )
        .thenAccept { item ->
          item.data.forEach {
            commandGateway.send<Any>(
                ExpireTodoCommand(it.aggregateId, it.todo),
            )
          }
        }
  }
}
