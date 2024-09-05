package de.eventsourcingbook.cart.changeprice.internal

import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.*

@RestController
class PriceChangeKafkaDebugResource(
    private var kafkaTemplate: KafkaTemplate<String, in ExternalPriceChangedEvent>,
) {
    var logger = KotlinLogging.logger {}

    @CrossOrigin
    @PostMapping("/debug/external/changeprice")
    fun processDebugCommand(
        @RequestParam productId: UUID,
        @RequestParam price: BigDecimal,
        @RequestParam oldPrice: BigDecimal,
    ) {
        kafkaTemplate.send("inventories", ExternalPriceChangedEvent(productId, price, oldPrice))
    }
}
