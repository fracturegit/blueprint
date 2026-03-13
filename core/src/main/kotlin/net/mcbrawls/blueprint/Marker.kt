package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.kyori.adventure.key.Key

/**
 * A named, typed element within a blueprint.
 *
 * A marker groups one or more [Anchor] positions under a single logical concept (e.g. all spawn
 * points for a team, a jump pad source, a KOTH objective), identified by a [Key] type that game
 * code registers against. Both the marker and its individual anchors carry typed [properties].
 *
 * Usage example (game code):
 * ```kotlin
 * val hills = placedBlueprint.getMarker("hills")
 * val defaultHill = hills?.anchors?.firstOrNull { it.properties.getValue("default")?.let { v -> (v as? PropertyValue.BoolValue)?.value } == true }
 * val domHills = hills?.anchors
 *     ?.filter { "dom_zone" in it.properties }
 *     ?.sortedBy { (it.properties.getValue("dom_zone") as? PropertyValue.IntValue)?.value }
 * ```
 */
data class Marker(
    val type: Key,
    val anchors: List<Anchor>,
    val properties: Map<String, PropertyValue> = emptyMap(),
) {
    companion object {
        val KEY_CODEC: Codec<Key> = Codec.STRING.xmap(Key::key, Key::asString)

        val CODEC: Codec<Marker> = RecordCodecBuilder.create { instance ->
            instance.group(
                KEY_CODEC.fieldOf("type").forGetter(Marker::type),
                Anchor.CODEC.listOf().optionalFieldOf("anchors", emptyList()).forGetter(Marker::anchors),
                PropertyValue.MAP_CODEC.optionalFieldOf("properties", emptyMap()).forGetter(Marker::properties),
            ).apply(instance, ::Marker)
        }
    }
}
