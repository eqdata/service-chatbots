package com.github.eqdata.cmd
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel

case class EnableBot(channel: IChannel) extends BotCommand {}

object EnableBot extends ShadyAdmin {
  def from(event: MessageReceivedEvent): Option[EnableBot] = {
    val guild = event.getGuild
    val author = event.getAuthor
    if (isAuthorised(author, guild)) Some(EnableBot(event.getMessage.getChannel)) else None
  }
}

case class DisableBot(channel: IChannel) extends BotCommand {}

object DisableBot extends ShadyAdmin {
  def from(event: MessageReceivedEvent): Option[DisableBot] = {
    val guild = event.getGuild
    val author = event.getAuthor
    if (isAuthorised(author, guild)) Some(DisableBot(event.getMessage.getChannel)) else None
  }
}
