package net.mcbrawls.blueprint

/**
 * A typed handle for reading a named property from a [Map<String, PropertyValue>].
 *
 * Define keys statically in the consumer module - Blueprint stays agnostic to what games store.
 *
 * ```kotlin
 * object KothProperties {
 *     val RADIUS   = PropertyKey.double("radius")
 *     val DEFAULT  = PropertyKey.bool("default")
 *     val DOM_ZONE = PropertyKey.int("dom_zone")
 * }
 *
 * object SpawnProperties {
 *     val INIT_INDEX = PropertyKey.int("init_index")
 * }
 *
 * // Read-side usage:
 * val radius: Double? = anchor.properties[KothProperties.RADIUS]
 * val zone: Int?      = anchor.properties[KothProperties.DOM_ZONE]
 * val isDefault: Boolean = anchor.properties[KothProperties.DEFAULT] ?: false
 * ```
 *
 * The string name remains the on-disk key; [PropertyKey] is a purely compile-time construct.
 */
class PropertyKey<T : Any>(
    val name: String,
    val default: T? = null,
    internal val extract: (PropertyValue) -> T?,
) {
    companion object {
        fun int(name: String, default: Int? = null) =
            PropertyKey(name, default) { (it as? PropertyValue.IntValue)?.value }

        fun double(name: String, default: Double? = null) =
            PropertyKey(name, default) { (it as? PropertyValue.DoubleValue)?.value }

        fun string(name: String, default: String? = null) =
            PropertyKey(name, default) { (it as? PropertyValue.StringValue)?.value }

        fun bool(name: String, default: Boolean? = null) =
            PropertyKey(name, default) { (it as? PropertyValue.BoolValue)?.value }
    }
}

/**
 * Reads the value for [key] from this property map, returning [PropertyKey.default] if absent or
 * the wrong type, or `null` if no default is set.
 */
fun <T : Any> Map<String, PropertyValue>.getValue(key: PropertyKey<T>): T? {
    val raw: PropertyValue? = this[key.name]
    return raw?.let { key.extract(it) } ?: key.default
}
