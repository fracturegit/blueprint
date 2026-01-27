package net.mcbrawls.fracture.blueprint.editor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.Blueprint
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

object BlueprintEditorHandler {
    private val logger: Logger = LoggerFactory.getLogger(BlueprintEditorHandler::class.java)
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(16, "blueprint-editors"))

    private val instances: MutableMap<UUID, BlueprintEditorInstance> = mutableMapOf()
    private val initializingInstances: MutableList<BlueprintEditorInstance> = mutableListOf()

    fun add(blueprint: Blueprint<Block>, id: Key, loadedCallback: (BlueprintEditorInstance) -> Unit) {
        val instance = BlueprintEditorInstance(id, blueprint)
        instance.setChunkSupplier(::LightingChunk)
        instance.time = 12000
        instance.timeRate = 0

        scope.launch {
            try {
                val instanceManager = MinecraftServer.getInstanceManager()
                instanceManager.registerInstance(instance)

                initialize(instance)

                instances[instance.uuid] = instance

                loadedCallback.invoke(instance)
                instance.postInitialize()
            } finally {
                initializingInstances.remove(instance)
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun initialize(instance: BlueprintEditorInstance) {
        val uuid = instance.uuid
        val blueprintId = instance.blueprintId

        logger.info("Initializing new blueprint editor of $blueprintId: $uuid")

        // notify instance
        instance.initializeInternal()
        instance.initializeEvents(instance.eventNode())
    }

    fun remove(instance: BlueprintEditorInstance) {
        logger.info("Removing blueprint editor instance ${instance.uuid}")
        val instanceManager = MinecraftServer.getInstanceManager()
        instanceManager.unregisterInstance(instance)
        instances.remove(instance.uuid)
    }

    fun save(instance: BlueprintEditorInstance) {
        logger.info("Saving blueprint editor instance ${instance.uuid}")
        instance.save(File("generated_blueprints"))
    }
}
