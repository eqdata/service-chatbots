package com.github.eqdata.cmd

import com.typesafe.config.ConfigFactory
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.{IChannel, IUser}

object BotCommand {

  private val conf = ConfigFactory.load()

  def parse(msg: MessageReceivedEvent): Option[_ <: BotCommand] =
    msg.getMessage.getContent.trim.split("\\s+").toSeq match {
      case "/shady" +: ("enable" | "on") +: Nil   => EnableBot.from(msg)
      case "/shady" +: ("disable" | "off") +: Nil => DisableBot.from(msg)
      case _                                      => None
    }
}

case class Message(channel: IChannel, text: String)

trait BotCommand
