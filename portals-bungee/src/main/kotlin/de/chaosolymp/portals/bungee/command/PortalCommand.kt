package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import java.util.*

class PortalCommand(private val plugin: BungeePlugin) : Command("portal") {

    internal val commandRegistry = mutableMapOf<String, SubCommand>()

    init {
        commandRegistry["create"] = CreateCommand(this.plugin)
        commandRegistry["link"] = LinkCommand(this.plugin)
        commandRegistry["list"] = ListCommand(this.plugin)
        commandRegistry["modify"] = ModifyCommand(this.plugin)
        commandRegistry["info"] = InfoCommand(this.plugin)
        commandRegistry["remove"] = RemoveCommand(this.plugin)
        commandRegistry["help"] = HelpCommand(this.plugin, this)
    }

    override fun execute(sender: CommandSender?, args: Array<out String>?) {
        val cmd = if(args!!.size < 0) {
            commandRegistry[args[0]] ?: commandRegistry["help"]
        } else {
            commandRegistry["help"]
        }

        cmd!!.execute(sender!!, Arrays.copyOfRange(args, 1, args.size))
    }

}