package de.eventsourcingbook.cart.application

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeviceFingerPrintCalculator {
    fun calculateDeviceFingerPrint(): String = UUID.randomUUID().toString()

    companion object {
        val DEFAULT_FINGERPRINT: String = "default-fingerprint"
    }
}
