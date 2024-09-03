package de.eventsourcingbook.cart.archiveitem.internal

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.domain.commands.archiveitem.ArchiveItemCommand
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

data class ArchiveItemPayload(var aggregateId: UUID, var productId: UUID)

@RestController
class ArchiveItemRessource(private var commandGateway: CommandGateway) {

  var logger = KotlinLogging.logger {}

    @CrossOrigin
    @PostMapping("/debug/archiveitem")
    fun processDebugCommand(
        @RequestParam aggregateId: UUID,
        @RequestParam productId: UUID
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(ArchiveItemCommand(aggregateId, productId))
    }

    @CrossOrigin
    @PostMapping("/archiveitem/{aggregateId}")
    fun processCommand(
        @PathVariable("aggregateId") aggregateId: UUID,
        @RequestBody payload: ArchiveItemPayload
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(
            ArchiveItemCommand(aggregateId = payload.aggregateId, productId = payload.productId)
        )
    }
}
