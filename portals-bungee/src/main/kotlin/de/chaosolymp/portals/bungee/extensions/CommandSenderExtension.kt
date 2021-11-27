package de.chaosolymp.portals.bungee.extensions

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.BaseComponent

fun CommandSender.sendMessage(message: Array<BaseComponent>?) {
    sendMessage(*message!!)
}