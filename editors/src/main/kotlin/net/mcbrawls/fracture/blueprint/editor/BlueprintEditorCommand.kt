package net.mcbrawls.fracture.blueprint.editor

import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.fracture.command.AbstractCommand
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block

class BlueprintEditorCommand(serializer: MinestomBlueprintSerializer, name: String, vararg aliases: String) : AbstractCommand(name, *aliases) {
    init {
        val blueprintArg = ArgumentType.Word("blueprint").let { arg ->
            arg.setSuggestionCallback { _, _, suggestion ->
                val blueprints = serializer.collectBlueprints()
                suggest(blueprints.keys, suggestion)
            }
        }

        addSyntax(exceptionalExecutor { sender, context ->
            verifyPlayer(sender, 2)
            val blueprintId = context[blueprintArg]
            val blueprintKey = Key.key(blueprintId)
            val blueprint = serializer[blueprintKey] ?: error("No blueprint: $blueprintKey")
            execute(sender, blueprint, blueprintKey)
        }, blueprintArg)
    }

    private fun execute(player: Player, blueprint: Blueprint<Block>, blueprintId: Key) {
        BlueprintEditorHandler.add(blueprint, blueprintId) { instance ->
            val size = blueprint.size
            player.setInstance(instance, BlueprintEditorInstance.ORIGIN.asPos().add(size.x / 2.0, size.y / 2.0, size.z / 2.0)).join()
            player.gameMode = GameMode.SPECTATOR
        }
    }
}
