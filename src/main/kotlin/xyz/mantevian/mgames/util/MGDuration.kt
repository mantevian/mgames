package xyz.mantevian.mgames.util

import kotlinx.serialization.Serializable

fun Int.ticks() = MGDuration.fromTicks(this)
fun Int.seconds() = MGDuration.fromSeconds(this)
fun Int.minutes() = MGDuration.fromMinutes(this)
fun Int.hours() = MGDuration.fromHours(this)

@Serializable(with = MGDurationSerializer::class)
class MGDuration private constructor(private var ticks: Int) {
	fun getTicks() = ticks
	fun getFullSeconds() = ticks / 20
	fun getFullMinutes() = ticks / (20 * 60)
	fun getFullHours() = ticks / (20 * 60 * 60)

	fun formatMinSec() =
		"${getFullMinutes().toString().padStart(2, '0')}:${(getFullSeconds() % 60).toString().padStart(2, '0')}"

	fun formatHourMinSec() = "${getFullHours().toString().padStart(2, '0')}:${
		(getFullMinutes() % 60).toString().padStart(2, '0')
	}:${(getFullSeconds() % 60).toString().padStart(2, '0')}"

	fun inc() {
		ticks++
	}

	fun setFrom(other: MGDuration) {
		this.ticks = other.ticks
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

	fun isExactlySeconds(seconds: Int): Boolean {
		return ticks % 20 == 0 && getFullSeconds() == seconds
	}

	operator fun plus(other: MGDuration) = MGDuration(this.ticks + other.ticks)
	operator fun minus(other: MGDuration) = MGDuration(this.ticks - other.ticks)

	override fun equals(other: Any?): Boolean {
		return other is MGDuration && this.ticks == other.ticks
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	companion object {
		fun zero() = MGDuration(0)

		fun fromTicks(ticks: Int) = MGDuration(ticks)

		fun fromSeconds(seconds: Int) = MGDuration(seconds * 20)

		fun fromMinutes(minutes: Int, seconds: Int = 0) = MGDuration((minutes * 60 + seconds) * 20)

		fun fromHours(hours: Int, minutes: Int = 0, seconds: Int = 0) =
			MGDuration((hours * 60 * 60 + minutes * 60 + seconds) * 20)

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