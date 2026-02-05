package net.mcbrawls.blueprint.state

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.Blueprint.Companion.logger
import java.util.Optional
import java.util.function.Consumer

data class State(
    val blockId: String,
    val properties: Map<String, String> = emptyMap(),
) {
    companion object {
        val CODEC: Codec<State> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("Name").forGetter(State::blockId),
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .lenientOptionalFieldOf("Properties")
                    .xmap({ statex -> statex.orElse(emptyMap()) }, Optional<State>::of)
                    .forGetter(State::properties),
            ).apply(instance, ::State)
        }.orElseGet(Consumer { error -> logger.error("Could not load blockstate: $error") }, State::empty)

        val empty = State("minecraft:air")
    }
}
