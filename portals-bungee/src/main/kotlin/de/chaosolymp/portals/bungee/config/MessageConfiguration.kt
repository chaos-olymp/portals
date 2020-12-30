package de.chaosolymp.portals.bungee.config

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.config.Configuration

class MessageConfiguration(private val config: Configuration) {

    companion object {
        fun getDefaultConfiguration(): Configuration {
            val config = Configuration()
            config.set("variables.prefix", "&8[&e&l!&r&8] &5&lPortal&r &8» &r")
            config.set("messages.error.no-permission", "%prefix% &cKeine Rechte")
            config.set("messages.error.not-a-player", "%prefix% &cDu bist kein Spieler")
            config.set("messages.error.database-error", "%prefix% &cDatenbankfehler - Bitte kontaktiere einen Administrator")
            config.set("messages.error.wrong-syntax", "%prefix% &cKorrekte Verwendung: {syntax}")
            config.set("messages.error.name-already-exists", "%prefix% &cDer Portalname existiert bereits. Bitte wähle einen anderen.")
            config.set("messages.error.wrong-name", "%prefix% &cDer Portalname darf NUR Kleinbuchstaben & Unterstriche (_) enthalten.")
            config.set("messages.error.no-access-to-linked-portal", "%prefix% &cDu hast keine Berechtigung auf das zu verlinkende Portal. Bitte stelle sicher, dass der Portalbesitzer dies nicht auf Privat gestellt hat um es zu verlinken.")
            config.set("messages.error.no-access-to-portal", "%prefix% &cDies ist nicht dein Portal.")
            config.set("messages.error.not-exists", "%prefix% &cDieses Portal existiert nicht.")
            config.set("messages.error.no-region-access", "%prefix% &cDu kannst hier kein Portal erstellen. Bitte überprüfe deine Berechtigungen hier sowie ob du auf einem Endportalrahmen stehst.")
            config.set("messages.error.origin-not-exists", "%prefix% &cDas Ursprungsportal existiert nicht.")
            config.set("messages.error.link-not-exists", "%prefix% &cDas Zielportal existiert nicht.")
            config.set("messages.error.pagination.unknown-number", "%prefix% &cDies ist keine valide Seitenzahl.")
            config.set("messages.error.pagination.not-exists", "%prefix% &cDiese Seite existiert nicht.")
            config.set("messages.command.link", "%prefix% &9Du hast erfolgreich das Portal &2{origin-name}&6#{origin-id} &9mit dem Portal &2{link-name}&6#{link-id} verknüpft.")
            config.set("messages.comman.d.create", "%prefix% &9Du hast erfolgreich das Portal &2{name} &9mit der Identifikationsnummer &6#{id} &9erstellt.")
            config.set("messages.command.remove", "%prefix% &9Du hast erfolgreich das Portal &2{name} &9mit der Identifikationsnummer &6#{id} &9entfernt.")
            config.set("messages.command.modify.public", "%prefix% &9Du hast das Portal &2{name}&6#{id} &9öffentlich gestellt.")
            config.set("messages.command.modify.private", "%prefix% &9Du hast das Portal &2{name}&6#{id} &9privat gestellt.")
            config.set("messages.command.modify.display-name", "%prefix% &9Du hast den Anzeigename des Portals &2{name}&6#{id} &9auf &2{display-name}&r &9gestellt.")
            config.set("messages.command.modify.name", "%prefix% &9Du hast das Portal &6#{id} &9von &2{origin-display-name}&r &9in &2{origin-display-name} &9unbenannt.")
            config.set("messages.command.remove", "%prefix% &9Du hast das Portal &2{name}&6#{id} &9entfernt.")
            config.set("messages.command.help.component", "%prefix% &3/portal {sub-command}")
            config.set("messages.command.list.header", "%prefix% &5Portale (Seite {current-page}/{max-pages})")
            config.set("messages.command.list.badge.all.text", "&c[Alle Portale]")
            config.set("messages.command.list.badge.all.hover", "&9Klicke hier um alle Portale aufzulisten")
            config.set("messages.command.list.badge.own.text", "&a[Deine Portale]")
            config.set("messages.command.list.badge.own.hover", "&9Klicke hier um deine Portale aufzulisten")
            config.set("messages.command.list.badge.public.text", "&6[Öffentliche Portale]")
            config.set("messages.command.list.badge.public.hover", "&9Klicke hier um alle öffentlichen Portale aufzulisten")
            config.set("messages.command.list.component", "%prefix% &6Name: {name}#{id} (&6Anzeigename: {display-name})")
            config.set("messages.command.list.hover", "%9Klicke hier für mehr Informationen über dieses Portal")
            config.set("messages.command.list.footer", "") // nothing
            config.set("messages.command.info", "\n%prefix% &6Name: {name}\n%prefix% &6Anzeigename: {display-name}\n%prefix% &6Id: {id}\n%prefix% &6Besitzer: {owner}\n%prefix% &6Öffentlich: {public}\n%prefix% &6Erstellt: {created}\n%prefix% &6Letzte Änderung: {updated}\n")

            return config
        }
    }

    fun getMessage(key: String, vararg replacements: Replacement): Array<BaseComponent> = TextComponent.fromLegacyText(this.getLanguageElement("messages.$key", *replacements), ChatColor.WHITE)

    private fun getVariable(key: String, vararg replacements: Replacement) = this.getLanguageElement("variables.$key", *replacements)

    private fun getAllVariableKeys(): MutableCollection<String>? = config.getSection("variables").keys

    private fun getLanguageElement(key: String, vararg replacements: Replacement): String {
        var string = ChatColor.translateAlternateColorCodes('&', this.config.getString(key)!!)

        replacements.forEach { string = string.replace("{${it.key}}", it.value.toString()) }
        this.getAllVariableKeys()?.forEach { string = if(string.contains("%$it%")) string.replace("%$it%", this.getVariable(it)) else string }

        return string
    }


}