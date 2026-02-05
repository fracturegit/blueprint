package net.mcbrawls.fracture.blueprint.test

import com.mojang.brigadier.Command
import net.mcbrawls.api.resource
import net.mcbrawls.blueprint.editor.BlueprintEditorCommand
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.fracture.command.AbstractCommand
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent

object Main {
    val serializer = MinestomBlueprintSerializer(resource("blueprints") ?: error("No blueprints directory"))

    @JvmStatic
    fun main(args: Array<String>) {
        val server = MinecraftServer.init(Auth.Online())

        val commandManager = MinecraftServer.getCommandManager()
        commandManager.register(BlueprintEditorCommand(serializer, "editor"))
        commandManager.register(object : AbstractCommand("gamemode") {
            init {
                addSyntax {
                    requireBase()

                    playerExecutor { player, _ ->
                        player.setGameMode(GameMode.CREATIVE)
                    }
                }
            }
        })

        val instance = MinecraftServer.getInstanceManager().createInstanceContainer()

        val events = MinecraftServer.getGlobalEventHandler()
        events.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
            event.spawningInstance = instance

            val player = event.player
            player.setGameMode(GameMode.SPECTATOR)
            player.permissionLevel = 4
        }
        events.addListener(PlayerGameModeRequestEvent::class.java) { event ->
            val player = event.player
            if (player.permissionLevel < 2) return@addListener
            player.setGameMode(event.requestedGameMode)
        }

        server.start("0.0.0.0", 25565)
    }
}
