package xyz.mantevian.mgames.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import net.minecraft.core.Vec3i

object MGDurationSerializer : KSerializer<MGDuration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("xyz.mantevian.mgames.util.MGDurationSerializer", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: MGDuration) {
        encoder.encodeInt(value.getTicks())
    }

    override fun deserialize(decoder: Decoder): MGDuration {
        val ticks = decoder.decodeInt()
        return MGDuration.fromTicks(ticks)
    }
}

object Vec3iSerializer : KSerializer<Vec3i> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Vec3") {
        element<Int>("x")
        element<Int>("y")
        element<Int>("z")
    }

    override fun serialize(encoder: Encoder, value: Vec3i) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
            encodeIntElement(descriptor, 2, value.z)
        }
    }

    override fun deserialize(decoder: Decoder): Vec3i {
        return decoder.decodeStructure(descriptor) {
            var x = 0
            var y = 0
            var z = 0
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeIntElement(descriptor, 0)
                    1 -> y = decodeIntElement(descriptor, 1)
                    2 -> z = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            Vec3i(x, y, z)
        }
    }
}