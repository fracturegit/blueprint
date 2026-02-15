package net.mcbrawls.blueprint.serialization

import com.mojang.serialization.Codec
import net.kyori.adventure.nbt.BinaryTagIO
import net.mcbrawls.blueprint.Blueprint
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
     * @see net.mcbrawls.blueprint.Blueprint.Companion.createCodec
     */
    val codec: Codec<Blueprint<T>>,

    /**
     * The root folder to read blueprints from.
     */
    val folderRoot: File,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val blueprints: MutableMap<String, Blueprint<T>> = mutableMapOf()

    fun reload(): Int {
        blueprints.clear()

        measureTime {
            folderRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    runCatching {
                        val ext = file.extension
                        if (ext == "nbt") {
                            val filePath = file.relativeTo(folderRoot).path
                            val namespace = filePath.substringBefore(File.separator)
                            val path = extractPath(filePath, ext, namespace)
                            val key = "$namespace:$path"

                            runCatching {
                                val blueprint = loadBlueprint(file)
                                blueprints[key] = blueprint
                            }.onFailure { throwable ->
                                logger.error("Failed to load blueprint: $key", throwable)
                            }
                        }
                    }
                }
        }.let { duration ->
            logger.info("Loaded ${blueprints.size} blueprints in ${duration.inWholeMilliseconds} ms")
        }

        return blueprints.size
    }

    private fun loadBlueprint(file: File): Blueprint<T> {
        return file.inputStream().use { stream ->
            val reader = BinaryTagIO.unlimitedReader()
            val tag = reader.read(stream, BinaryTagIO.Compression.GZIP)
            codec.decodeQuick(NbtOps.INSTANCE, tag) ?: error("Parse error: $file")
        }
    }

    fun reload(id: String): Blueprint<T>? {
        val file = getFile(id)
        if (!file.exists()) return null

        val blueprint = loadBlueprint(file)
        blueprints[id] = blueprint
        return blueprint
    }

    fun getFile(id: String): File {
        val path = id
            .replace(":", "/")
            .replace("/", File.separator)

        return folderRoot.resolve("$path.nbt")
    }

    private fun extractPath(path: String, ext: String, namespace: String): String =
        path.removeSuffix(".$ext")
            .substringAfter(namespace)
            .removePrefix(File.separator)
            .split(File.separator)
            .joinToString("/")

    open operator fun get(id: String): Blueprint<T>? {
        return blueprints[id]
    }

    fun collectBlueprints(): Map<String, Blueprint<T>> {
        return blueprints.toMap()
    }
}
