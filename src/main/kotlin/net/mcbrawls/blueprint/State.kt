package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class State(
    val blockId: String,
    val properties: Map<String, String> = emptyMap(),
) {
    companion object {
        val CODEC: Codec<State> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("Name").forGetter(State::blockId),
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .fieldOf("Properties")
                    .orElse(emptyMap())
                    .forGetter(State::properties),
            ).apply(instance, ::State)
        }

        val empty = State("minecraft:air")
    }
}
