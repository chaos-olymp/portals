package de.chaosolymp.portals.bungee.command

import net.md_5.bungee.api.CommandSender

interface SubCommand {
    suspend fun execute(sender: CommandSender, args: Array<out String>?)
}