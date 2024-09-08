package de.eventsourcingbook.cart.support

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.*

@Converter(autoApply = true) // Automatically apply this converter to all UUID fields
class UUIDToStringConverter : AttributeConverter<UUID?, String?> {
    override fun convertToDatabaseColumn(uuid: UUID?): String? {
        return uuid?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): UUID? {
        return if (dbData != null) UUID.fromString(dbData) else null
    }
}
