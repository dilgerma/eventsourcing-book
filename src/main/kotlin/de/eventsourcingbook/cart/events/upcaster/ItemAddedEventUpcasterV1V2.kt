package de.eventsourcingbook.cart.events.upcaster

import com.fasterxml.jackson.databind.node.ObjectNode
import de.eventsourcingbook.cart.application.DeviceFingerPrintCalculator
import org.axonframework.serialization.SimpleSerializedType
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation
import org.axonframework.serialization.upcasting.event.SingleEventUpcaster
import org.junit.jupiter.api.Order
import org.springframework.stereotype.Component

@Component
@Order(0)
class ItemAddedEventUpcasterV1V2 : SingleEventUpcaster() {
    override fun canUpcast(intermediateRepresentation: IntermediateEventRepresentation?): Boolean {
        //
        return intermediateRepresentation?.type?.equals(SOURCE_TYPE) ?: false
    }

    override fun doUpcast(intermediateRepresentation: IntermediateEventRepresentation): IntermediateEventRepresentation =
        intermediateRepresentation.upcastPayload(
            SimpleSerializedType(TARGET_TYPE.name, "2.0"),
            com.fasterxml.jackson.databind.JsonNode::class.java,
            { item ->
                (item as ObjectNode).put(
                    de.eventsourcingbook.cart.events.ItemAddedEvent::deviceFingerPrint.name,
                    DeviceFingerPrintCalculator.DEFAULT_FINGERPRINT,
                )
                item
            },
        )

    companion object {
        val SOURCE_TYPE: SimpleSerializedType =
            SimpleSerializedType(de.eventsourcingbook.cart.events.versioned.ItemAddedEvent::class.java.getTypeName(), null)
        val TARGET_TYPE: SimpleSerializedType =
            SimpleSerializedType(de.eventsourcingbook.cart.events.ItemAddedEvent::class.java.getTypeName(), "2.0")
    }
}
