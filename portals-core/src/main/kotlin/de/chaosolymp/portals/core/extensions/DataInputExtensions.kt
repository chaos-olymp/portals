package de.chaosolymp.portals.core.extensions

import de.chaosolymp.portals.core.UUIDUtils
import java.io.DataInput
import java.util.*

fun DataInput.readUUID(): UUID {
    val uuidArray = ByteArray(16)
    readFully(uuidArray)
    return UUIDUtils.getUUIDFromBytes(uuidArray)
}