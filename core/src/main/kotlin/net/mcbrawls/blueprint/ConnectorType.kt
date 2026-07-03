package net.mcbrawls.blueprint

enum class ConnectorType {
    ENTRANCE,
    EXIT,
    BOTH;

    /** True when this connector can accept an incoming room (entrance role). */
    val isEntrance: Boolean get() = this == ENTRANCE || this == BOTH

    /** True when this connector can spawn a further room (exit role). */
    val isExit: Boolean get() = this == EXIT || this == BOTH
}
