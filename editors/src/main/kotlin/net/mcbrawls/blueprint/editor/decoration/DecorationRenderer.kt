package net.mcbrawls.blueprint.editor.decoration

import net.mcbrawls.blueprint.Decoration
import net.minestom.server.entity.Entity
import net.minestom.server.instance.InstanceContainer

interface DecorationRenderer {
    /**
     * Spawns a visual preview entity for [decoration] in [instance].
     * The caller will attach the returned entity as a passenger of the [DecorationEntity].
     * Return null to fall back to the text-display label only.
     */
    fun spawnPreview(instance: InstanceContainer, decoration: Decoration): Entity?

    /**
     * Removes a preview entity previously returned by [spawnPreview].
     */
    fun removePreview(entity: Entity)
}
