package xyz.mantevian.mgames.util

import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import xyz.mantevian.mgames.MOD_ID
import xyz.mantevian.mgames.json

class MGResourceManager : ResourceManagerReloadListener {
    val values: MutableMap<String, String> = mutableMapOf()

    init {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.parse("mantevian:$MOD_ID"), this)
    }

    inline fun <reified T> get(key: String): T? {
        return values[key.replace("$MOD_ID/", "")]?.let { json.decodeFromString(it) }
    }

    override fun onResourceManagerReload(manager: ResourceManager) {
        val resources = manager.listResources(MOD_ID) { true }
        resources.forEach { (key, resource) ->
            resource.open().use { stream ->
                values[key.path.replace("$MOD_ID/", "")] = stream.readAllBytes().decodeToString()
            }
        }
    }
}