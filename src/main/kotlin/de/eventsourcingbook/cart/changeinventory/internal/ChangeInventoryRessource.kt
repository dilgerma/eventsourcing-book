package de.eventsourcingbook.cart.changeinventory.internal

import de.eventsourcingbook.cart.common.CommandResult
import de.eventsourcingbook.cart.domain.commands.changeinventory.ChangeInventoryCommand
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.concurrent.CompletableFuture

data class ChangeInventoryPayload(var inventory: Int, var productId: UUID)

@RestController
class ChangeInventoryRessource(private var commandGateway: CommandGateway) {

    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @PostMapping("/debug/changeinventory")
    fun processDebugCommand(
        @RequestParam productId: UUID,
        @RequestParam inventory: Int,
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(
            ChangeInventoryCommand(
                productId,
                inventory,
            ),
        )
    }

    @CrossOrigin
    @PostMapping("/changeinventory/{aggregateId}")
    fun processCommand(
        @PathVariable("aggregateId") aggregateId: UUID,
        @RequestBody payload: ChangeInventoryPayload,
    ): CompletableFuture<CommandResult> {
        return commandGateway.send(
            ChangeInventoryCommand(
                productId = payload.productId,
                inventory = payload.inventory,
            ),
        )
    }
}
