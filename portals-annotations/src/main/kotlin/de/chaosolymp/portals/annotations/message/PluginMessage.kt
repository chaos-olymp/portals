package de.chaosolymp.portals.annotations.message

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PluginMessage(val indicator: String)
