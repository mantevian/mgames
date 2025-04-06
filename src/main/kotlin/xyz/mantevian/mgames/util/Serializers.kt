package xyz.mantevian.mgames.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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