package de.eventsourcingbook.cart.removeitem.internal

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.domain.commands.removeitem.RemoveItemCommand
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

data class RemoveItemPayload(var aggregateId: UUID, var itemId: UUID)

@RestController
class RemoveItemRessource(private var commandGateway: CommandGateway) {

  var logger = KotlinLogging.logger {}

  @CrossOrigin
  @PostMapping("/debug/removeitem")
  fun processDebugCommand(
      @RequestParam aggregateId: UUID,
      @RequestParam itemId: UUID
  ): CompletableFuture<CommandResult> {
    return commandGateway.send(RemoveItemCommand(aggregateId, itemId))
  }

  @CrossOrigin
  @PostMapping("/removeitem/{aggregateId}")
  fun processCommand(
      @PathVariable("aggregateId") aggregateId: UUID,
      @RequestBody payload: RemoveItemPayload
  ): CompletableFuture<CommandResult> {
    return commandGateway.send(
        RemoveItemCommand(aggregateId = payload.aggregateId, itemId = payload.itemId))
  }
}
