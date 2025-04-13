package xyz.mantevian.mgames.util

import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.json

class ResourceManager : SimpleSynchronousResourceReloadListener {
	val values: MutableMap<String, String> = mutableMapOf()

	init {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(this)
	}

	inline fun <reified T> get(key: String): T? {
		return values[key.replace("$MOD_ID/", "")]?.let { json.decodeFromString(it) }
	}

	override fun reload(manager: ResourceManager) {
		val resources = manager.findResources(MOD_ID) { true }
		resources.forEach { (key, resource) ->
			resource.inputStream.use { stream ->
				values[key.path.replace("$MOD_ID/", "")] = stream.readAllBytes().decodeToString()
			}
		}
	}

	override fun getFabricId(): Identifier {
		return Identifier.of("mantevian", "$MOD_ID/resources")
	}
}