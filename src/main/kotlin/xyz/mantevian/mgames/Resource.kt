package xyz.mantevian.mgames

import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier

class Resource(val path: String) : SimpleSynchronousResourceReloadListener {
	companion object {
		private val registeredPaths: Set<String> = mutableSetOf()
	}

	var str: String = ""
		private set

	init {
		if (registeredPaths.contains(path)) {
			throw RuntimeException("Resource $path is already registered")
		} else {
			registeredPaths.plus(path)
		}

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(this)
	}

	inline fun <reified T> get(json: Json): T {
		return json.decodeFromString(str)
	}

	override fun reload(manager: ResourceManager) {
		val resource = manager.getResource(fabricId)

		if (resource.isEmpty) {
			println("Resource $path is empty")
			return
		}

		resource.get().inputStream.use {
			str = it.readAllBytes().decodeToString()
		}
	}

	override fun getFabricId(): Identifier {
		return Identifier.of("mantevian", path)
	}
}