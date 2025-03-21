package xyz.mantevian.mgames

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MGDurationSerializer : KSerializer<MGDuration> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MGDuration", PrimitiveKind.INT)

	override fun serialize(encoder: Encoder, value: MGDuration) {
		encoder.encodeInt(value.getTicks())
	}

	override fun deserialize(decoder: Decoder): MGDuration {
		val ticks = decoder.decodeInt()
		return MGDuration.fromTicks(ticks)
	}
}

@Serializable(with = MGDurationSerializer::class)
class MGDuration private constructor(private var ticks: Int) {
	fun getTicks() = ticks
	fun getFullSeconds() = ticks / 20
	fun getFullMinutes() = ticks / (20 * 60)
	fun getFullHours() = ticks / (20 * 60 * 60)

	fun formatMinSec() = "${getFullMinutes().toString().padStart(2, '0')}:${(getFullSeconds() % 60).toString().padStart(2, '0')}"
	fun formatHourMinSec() = "${getFullHours().toString().padStart(2, '0')}:${(getFullMinutes() % 60).toString().padStart(2, '0')}:${(getFullSeconds() % 60).toString().padStart(2, '0')}"

	fun inc() {
		ticks++
	}

	fun set(i: Int) {
		ticks = i
	}

	fun set(formattedString: String): Boolean {
		val (ok, dur) = fromFormattedTime(formattedString)
		if (ok) {
			ticks = dur.getTicks()
		}
		return ok
	}

	fun clone(): MGDuration {
		return MGDuration(this.getTicks())
	}

	companion object {
		fun zero() = MGDuration(0)

		fun fromTicks(ticks: Int) = MGDuration(ticks)

		fun fromSeconds(seconds: Int) = MGDuration(seconds * 20)

		fun fromMinutes(minutes: Int, seconds: Int = 0) = MGDuration((minutes * 60 + seconds) * 20)

		fun fromHours(hours: Int, minutes: Int = 0, seconds: Int = 0) = MGDuration((hours * 60 * 60 + minutes * 60 + seconds) * 20)

		fun fromFormattedTime(input: String): Pair<Boolean, MGDuration> = input.run {
			val parts = split(":")

			return@run when (parts.size) {
				2 -> true to fromSeconds(parts[0].toInt() * 60 + parts[1].toInt())
				3 -> true to fromSeconds(parts[0].toInt() * 60 * 60 + parts[1].toInt() * 60 + parts[2].toInt())
				else -> false to zero()
			}
		}
	}
}