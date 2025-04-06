package xyz.mantevian.mgames.game

interface GameComponent {
	fun init() {}
	fun tick() {}
	fun canStart(): Boolean = true
	fun start() {}
	fun finish() {}
}