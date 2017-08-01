package com.github.eqdata

import akka.actor.{Actor, ActorSystem}
import com.github.eqdata.AuctionAgent.User
import com.github.eqdata.cmd.{BotCommand, DisableBot, EnableBot, PostAuction}
import com.typesafe.scalalogging.LazyLogging
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.MessageBuilder

import scala.language.postfixOps

class DiscordPublisher(token: String)(implicit system: ActorSystem) extends Actor with LazyLogging {

  override def receive: Receive = initialising

  private def initialising: Receive = {
    case Start =>
      logger.trace(s"starting $self")
      val client = new ClientBuilder().withToken(token).login()
      client.getDispatcher.registerListener(new IListener[MessageReceivedEvent] {
        override def handle(event: MessageReceivedEvent): Unit = BotCommand.parse(event).foreach { cmd =>
          self ! cmd
        }
      })
      sender ! Started
      context.become(running(client))
  }

  private def running(client: IDiscordClient, subscribedChannels: Set[IChannel] = Set.empty): Receive = {
    case SendMessage(msg, channel) =>
      new MessageBuilder(client).withChannel(channel).withContent(msg).build

    case EnableBot(channel) =>
      if (!subscribedChannels.contains(channel)) {
        self ! SendMessage(s"Starting auction feed", channel)
        context.become(running(client, subscribedChannels + channel))
      }

    case DisableBot(channel) =>
      if (subscribedChannels.contains(channel)) {
        self ! SendMessage(s"Steps into the shadows and fades away", channel)
        context.become(running(client, subscribedChannels + channel))
      }

    case PostAuction(User(name), items) =>
      val msg = items
        .map { i =>
          s"https://wiki.project1999.com/${i.uri}"
        }
        .toList
        .sorted
        .mkString(s"**$name** is selling ", " ", "")

      subscribedChannels.foreach(ch => self ! SendMessage(msg, ch))

  }

}

case class SendMessage(msg: String, channel: IChannel)
