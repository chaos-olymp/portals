package de.chaosolymp.portals.core

data class LocationResponse(val canCreatePortal: Boolean, val world: String, val x: Int, val y: Int, val z: Int, val yaw: Float, val pitch: Float)