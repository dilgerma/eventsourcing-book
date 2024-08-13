package de.eventsourcingbook.cart.changeinventory.internal

import java.util.*
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InventoryKafkaDebugResource(
    private var kafkaTemplate: KafkaTemplate<String, ExternalInventoryChangedEvent>
) {

  var logger = KotlinLogging.logger {}

  @CrossOrigin
  @PostMapping("/debug/external/changeinventory")
  fun processDebugCommand(
      @RequestParam productId: UUID,
      @RequestParam inventory: Int,
  ) {
    kafkaTemplate.send("inventories", ExternalInventoryChangedEvent(productId, inventory))
  }
}
