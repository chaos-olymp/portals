package de.chaosolymp.portals.core.extension

import de.chaosolymp.portals.core.UUIDUtils
import java.sql.ResultSet
import java.util.*

fun ResultSet.getUUID(columnLabel: String): UUID {
    return UUIDUtils.getUUIDFromBytes(getBytes(columnLabel))
}