package de.chaosolymp.portals.annotations.messages

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PluginMessage(val indicator: String)
