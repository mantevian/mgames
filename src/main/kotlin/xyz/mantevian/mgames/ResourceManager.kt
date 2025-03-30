package xyz.mantevian.mgames

import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier

class ResourceManager : SimpleSynchronousResourceReloadListener {
	val registeredFilePaths: MutableSet<String> = mutableSetOf()
	val registeredDirPaths: MutableSet<String> = mutableSetOf()
	val values: MutableMap<String, String> = mutableMapOf()

	init {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(this)
	}

	inline fun <reified T> get(key: String, json: Json): T? {
		return values[key.replace("${Main.MOD_ID}/", "")]?.let { json.decodeFromString(it) }
	}

	fun registerFile(path: String): Boolean {
		if (registeredFilePaths.contains(path)) {
			return false
		} else {
			registeredFilePaths.add(path)
			return true
		}
	}

	fun registerDir(path: String): Boolean {
		if (registeredDirPaths.contains(path)) {
			return false
		} else {
			registeredDirPaths.add(path)
			return true
		}
	}

	override fun reload(manager: ResourceManager) {
		registeredFilePaths.forEach { path ->
			val resource = manager.getResource(Identifier.of("mantevian", "${Main.MOD_ID}/$path")).get()
			resource.inputStream.use { stream ->
				values[path.replace("${Main.MOD_ID}/", "")] = stream.readAllBytes().decodeToString()
			}
		}

		registeredDirPaths.forEach { path ->
			val resources = manager.findResources("${Main.MOD_ID}/$path") { true }
			resources.forEach { (key, resource) ->
				resource.inputStream.use { stream ->
					values[key.path.replace("${Main.MOD_ID}/", "")] = stream.readAllBytes().decodeToString()
				}
			}
		}
	}

	override fun getFabricId(): Identifier {
		return Identifier.of("mantevian", "${Main.MOD_ID}/resources")
	}
}