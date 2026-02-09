package net.mcbrawls.blueprint.minestom

import net.mcbrawls.fracture.command.AbstractCommand

class ReloadBlueprintsCommand(serializer: MinestomBlueprintSerializer, name: String, vararg aliases: String) : AbstractCommand(name, *aliases) {
    init {
        addSyntax {
            requireBase()

            executor { sender, _ ->
                sender.sendMessage("Reloading all blueprints for ${serializer.name}")
                val count = serializer.reload()
                sender.sendMessage("Reloaded $count blueprints")
            }
        }
    }
}
