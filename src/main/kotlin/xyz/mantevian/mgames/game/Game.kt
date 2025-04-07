package xyz.mantevian.mgames.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.mantevian.mgames.util.GameState
import xyz.mantevian.mgames.util.MGDuration
import xyz.mantevian.mgames.util.resetPlayersMinecraftStats
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

@Serializable
class Game {
	@SerialName("state")
	var state: GameState = GameState.NOT_INIT

	@SerialName("time")
	var time: MGDuration = MGDuration.zero()

	@Serializable
	@SerialName("components")
	private val components: MutableSet<GameComponent> = mutableSetOf()

	fun <T : GameComponent> getComponent(componentClass: KClass<T>): GameComponent? {
		return components.find { it::class.isSubclassOf(componentClass) }
	}

	inline fun <reified T : GameComponent> getComponent(): T? {
		return getComponent(T::class) as T?
	}

	inline fun <reified T : GameComponent> getComponentOrDefault(): T {
		return (getComponent(T::class) as? T) ?: T::class.primaryConstructor!!.call()
	}

	inline fun <reified T : GameComponent> getComponentOrSetDefault(): T {
		if (!hasComponent(T::class)) {
			setComponent(T::class.primaryConstructor!!.call())
		}

		return getComponent(T::class) as T
	}

	fun <T : GameComponent> setComponent(component: T) {
		components.add(component)
	}

	fun setComponents(vararg components: GameComponent) {
		components.forEach {
			setComponent(it)
		}
	}

	fun <T : GameComponent> hasComponent(componentClass: KClass<T>): Boolean {
		return components.any { it::class.isSubclassOf(componentClass) }
	}

	inline fun <reified T : GameComponent> hasComponent(): Boolean {
		return hasComponent(T::class)
	}

	fun <T : GameComponent> removeComponent(componentClass: KClass<T>) {
		components.removeIf { it::class.isSubclassOf(componentClass) }
	}

	inline fun <reified T : GameComponent> removeComponent() {
		removeComponent(T::class)
	}

	inline fun <reified T : GameComponent> toggleComponent(flag: Boolean) {
		if (flag) {
			setComponent(T::class.primaryConstructor!!.call())
		} else {
			removeComponent(T::class)
		}
	}

	fun init() {
		resetPlayersMinecraftStats()
		state = GameState.WAITING
		components.forEach { it.init() }
	}

	fun tick() {
		components.forEach { it.tick() }
	}

	fun start() {
		components.forEach { it.start() }
	}

	fun canStart(): Boolean {
		return components.all { it.canStart() }
	}

	fun finish() {
		components.forEach { it.finish() }
	}
}