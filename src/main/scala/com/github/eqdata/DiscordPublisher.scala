package com.github.eqdata

import akka.actor.{Actor, ActorSystem}
import com.github.eqdata.AuctionAgent.User
import com.github.eqdata.cmd.{BotCommand, DisableBot, EnableBot, PostAuction}
import com.typesafe.scalalogging.LazyLogging
import sx.blah.discord.api.events.IListener
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.{EmbedBuilder, MessageBuilder}

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
      val mb = new MessageBuilder(client).withChannel(channel)
      (msg match {
        case Left(str) => mb.withContent(str)
        case Right(embed) => mb.withEmbed(embed)
      }).build

    case EnableBot(channel) =>
      if (!subscribedChannels.contains(channel)) {
        self ! SendMessage(Left("Starting auction feed"), channel)
        context.become(running(client, subscribedChannels + channel))
      }

    case DisableBot(channel) =>
      if (subscribedChannels.contains(channel)) {
        self ! SendMessage(Left("Steps into the shadows and fades away"), channel)
        context.become(running(client, subscribedChannels - channel))
      }

    case PostAuction(User(name), items) =>
      val msg = items.foldLeft(new EmbedBuilder().withTitle(s"**$name** auctions")) { case (m, i) =>
          m.appendField("selling", s"[${i.name}](https://wiki.project1999.com/${i.uri})", true)
      }.build()
      subscribedChannels.foreach(ch => self ! SendMessage(Right(msg), ch))
  }
}

case class SendMessage(msg: Either[String, EmbedObject], channel: IChannel)
