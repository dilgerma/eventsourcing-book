package de.nebulit.automation.internal

import de.nebulit.domain.commands.automation.ExpireTodoCommand
import java.util.UUID
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class AutomationPayload(var aggregateId: UUID, var todo: String)

@RestController
class ExpireTodoRessource(private var commandGateway: CommandGateway) {

  var logger = KotlinLogging.logger {}

  @CrossOrigin
  @PostMapping("/debug/automation")
  fun processDebugCommand(
      @RequestParam aggregateId: UUID,
      @RequestParam todo: String
  ): CompletableFuture<Any> {
    return commandGateway.send(ExpireTodoCommand(aggregateId, todo))
  }

  @CrossOrigin
  @PostMapping("/automation/{aggregateId}")
  fun processCommand(
      @PathVariable("aggregateId") aggregateId: UUID,
      @RequestBody payload: AutomationPayload
  ): CompletableFuture<Any> {
    return commandGateway.send(
        ExpireTodoCommand(aggregateId = payload.aggregateId, todo = payload.todo))
  }
}
