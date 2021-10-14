package de.chaosolymp.portals.core.extensions

import de.chaosolymp.portals.core.UUIDUtils
import java.io.DataOutput
import java.util.*

fun DataOutput.writeUUID(uuid: UUID) {
    write(UUIDUtils.getBytesFromUUID(uuid))
}