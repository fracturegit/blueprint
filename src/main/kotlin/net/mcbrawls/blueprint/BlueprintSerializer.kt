package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import net.kyori.adventure.nbt.BinaryTagIO
import net.mcbrawls.blueprint.util.NbtOps
import net.mcbrawls.codex.decodeQuick
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.measureTime

/**
 * Loads blueprints from the given folder.
 */
open class BlueprintSerializer<T>(
    /**
     * The codec to serialize blueprints.
     * @see Blueprint.createCodec
     */
    val codec: Codec<Blueprint<T>>,

    /**
     * The root folder to read blueprints from.
     */
    val folderRoot: File,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val blueprints: MutableMap<String, Blueprint<T>> = mutableMapOf()

    fun load() {
        blueprints.clear()

        measureTime {
            folderRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    if (file.extension == "nbt") {
                        val filePath = file.relativeTo(folderRoot).path
                        val namespace = filePath.substringBefore(File.separator)
                        val path = filePath.removeSuffix(".${file.extension}")
                            .substringAfter(namespace)
                            .removePrefix(File.separator)
                            .split(File.separator)
                            .joinToString("/")
                        val key = "$namespace:$path"

                        val tag = BinaryTagIO.unlimitedReader().read(file.inputStream(), BinaryTagIO.Compression.GZIP)
                        val blueprint = codec.decodeQuick(NbtOps.INSTANCE, tag) ?: error("Could not parse blueprint: $key")
                        blueprints[key] = blueprint
                    }
                }
        }.let { duration ->
            logger.info("Loaded ${blueprints.size} blueprints in ${duration.inWholeMilliseconds} ms")
        }
    }

    open operator fun get(id: String): Blueprint<T>? {
        return blueprints[id]
    }
}
