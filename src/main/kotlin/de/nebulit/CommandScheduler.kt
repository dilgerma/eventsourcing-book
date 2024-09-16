package de.nebulit

import de.nebulit.domain.commands.AddTodoCommand
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommandScheduler {
    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Scheduled(fixedDelay = 1000)
    fun addTodos() {
        commandGateway.sendAndWait<Any>(AddTodoCommand(AGGREGATE_ID, "todo"))
    }

    val AGGREGATE_ID = UUID.randomUUID()
}
