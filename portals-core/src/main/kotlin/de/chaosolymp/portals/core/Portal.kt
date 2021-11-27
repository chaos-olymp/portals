package de.chaosolymp.portals.core

import java.sql.Timestamp
import java.util.*

data class Portal(val id: Int, val owner: UUID?, val name: String, val displayName: String?, val public: Boolean, val created: Timestamp, val updated: Timestamp = created, val server: String, val world: String, val x: Int, val y: Int, val z: Int, val link: Int?)