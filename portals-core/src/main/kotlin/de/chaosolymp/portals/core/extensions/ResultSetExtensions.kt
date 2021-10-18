package de.chaosolymp.portals.core.extensions

import de.chaosolymp.portals.core.UUIDUtils
import java.sql.ResultSet
import java.util.*

fun ResultSet.getUUID(columnLabel: String): UUID {
    return UUIDUtils.getUUIDFromBytes(getBytes(columnLabel))
}