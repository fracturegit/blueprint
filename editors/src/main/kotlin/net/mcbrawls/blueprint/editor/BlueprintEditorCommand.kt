package net.mcbrawls.blueprint.editor

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.fracture.command.AbstractCommand
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block

class BlueprintEditorCommand(serializer: MinestomBlueprintSerializer, name: String, vararg aliases: String) : AbstractCommand(name, *aliases) {
    init {
        val blueprintArg = ArgumentType.String("blueprint").let { arg ->
            arg.setSuggestionCallback { _, _, suggestion ->
                val blueprints = serializer.collectBlueprints()
                suggest(blueprints.keys, suggestion)
            }
        }

        addSyntax {
            requireBase()
            args(ArgumentType.Literal("open"), blueprintArg)

            playerExecutor { player, context ->
                val blueprintId = context[blueprintArg]
                val blueprintKey = Key.key(blueprintId)
                val blueprint = serializer[blueprintKey]
                executeOpen(player, blueprint, blueprintKey)
            }
        }

        addSyntax {
            requireBase()

            args(ArgumentType.Literal("save"))

            playerExecutor { player, context ->
                val instance = player.instance as? BlueprintEditorInstance ?: error("Not in blueprint editor")
                executeSave(player, instance, null)
            }
        }

        addSyntax {
            requireBase()

            args(ArgumentType.Literal("save"), blueprintArg)

            playerExecutor { player, context ->
                val instance = player.instance as? BlueprintEditorInstance ?: error("Not in blueprint editor")
                val blueprintKey = Key.key(context[blueprintArg])
                executeSave(player, instance, blueprintKey)
            }
        }
    }

    private fun executeOpen(player: Player, blueprint: Blueprint<Block>?, blueprintId: Key) {
        BlueprintEditorHandler.add(blueprint, blueprintId) { instance ->
            val rootPos = BlueprintEditorInstance.ORIGIN.asPos()
            val pos = blueprint?.size?.let { size ->
                rootPos.add(size.x() / 2.0, size.y() / 2.0, size.z() / 2.0)
            } ?: rootPos

            player.setInstance(instance, pos).join()
            player.gameMode = GameMode.SPECTATOR
        }

        player.sendMessage("Opening blueprint editor: $blueprintId")
    }

    private fun executeSave(player: Player, instance: BlueprintEditorInstance, customBlueprintId: Key?) {
        customBlueprintId?.let { instance.blueprintId = it }
        BlueprintEditorHandler.save(instance)
        player.sendMessage(Component.text("Saved blueprint: ${instance.blueprintId}"))
    }
}
