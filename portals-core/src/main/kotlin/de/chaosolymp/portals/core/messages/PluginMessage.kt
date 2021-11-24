package de.chaosolymp.portals.core.messages

import java.io.DataOutput

interface PluginMessage {
    fun serialize(output: DataOutput)
    // TODO: Implement serialization and deserialization with code generator
    // See https://www.willowtreeapps.com/craft/generating-code-via-annotations-in-kotlin
}