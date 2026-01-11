package net.mcbrawls.blueprint.util

import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag

object NbtHelpers {
    @JvmStatic
    fun CompoundBinaryTag.entrySet(): Map<String, BinaryTag> {
        return keySet().associateWith {
            this[it] ?: error("Key $it not present but present in key set")
        }
    }

    @JvmStatic
    fun CompoundBinaryTag.shallowCopy(): CompoundBinaryTag {
        return CompoundBinaryTag.from(entrySet())
    }
}
