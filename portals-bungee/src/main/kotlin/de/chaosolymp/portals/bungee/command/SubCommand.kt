package de.chaosolymp.portals.bungee.command

import net.md_5.bungee.api.CommandSender

interface SubCommand {
    fun execute(sender: CommandSender, args: Array<out String>?)
}