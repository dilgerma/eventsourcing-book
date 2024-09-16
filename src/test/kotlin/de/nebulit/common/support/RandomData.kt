package de.nebulit.common.support

import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.util.*
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.randomizers.number.BigDecimalRandomizer
import org.jeasy.random.randomizers.text.StringRandomizer

object RandomData {

  inline fun <reified T> newInstance(
      fieldsToIgnore: List<String> = emptyList(),
      block: T.() -> Unit = {},
  ): T {
    val parameters =
        EasyRandomParameters()
            .collectionSizeRange(1, 4)
            .randomize(UUID::class.java) { UUID.randomUUID() }
            .randomize(BigDecimal::class.java, BigDecimalRandomizer(2, RoundingMode.CEILING))
            .randomize(CharSequence::class.java) { StringBuilder(StringRandomizer().randomValue) }
            .randomize(ByteBuffer::class.java) {
              ByteBuffer.wrap(StringRandomizer().randomValue.toByteArray())
            }
            .also { it.fieldExclusionPredicates.addAll(fieldsToIgnore.map(FieldPredicates::named)) }
    val generator = EasyRandom(parameters)

    var instance = generator.nextObject(T::class.java)
    block(instance)
    return instance.apply(block)
  }
}
