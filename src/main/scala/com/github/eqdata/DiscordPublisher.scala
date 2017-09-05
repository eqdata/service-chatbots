package com.github.eqdata

import akka.actor.{Actor, ActorSystem}
import com.github.eqdata.cmd.{BotCommand, DisableBot, EnableBot, PostAuction}
import com.typesafe.scalalogging.LazyLogging
import sx.blah.discord.api.events.IListener
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.{EmbedBuilder, MessageBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class DiscordPublisher(token: String)(implicit system: ActorSystem) extends Actor with LazyLogging {

  private val MaxAuctionsPerMessage = 10

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
      system.scheduler.schedule(2.seconds, 2.seconds, self, WriteAuctions)
      context.become(running(client))
  }

  private def writeAuctions(client: IDiscordClient, subscribedChannels: Set[IChannel], auctions: List[PostAuction]) {
    val msg = auctions
      .foldRight(new EmbedBuilder()) {
        case (PostAuction(user, items), m) =>
          val itemsString =
            items.toSeq.map(i => s"[${i.name}](https://wiki.project1999.com/${i.uri})").sorted.mkString("\n")
          m.appendField(user.name, itemsString, false)
      }
      .build()
    subscribedChannels.foreach(ch => self ! SendMessage(Right(msg), ch))
  }

  private def running(client: IDiscordClient,
                      subscribedChannels: Set[IChannel] = Set.empty,
                      auctions: List[PostAuction] = Nil): Receive = {

    case SendMessage(msg, channel) =>
      val mb = new MessageBuilder(client).withChannel(channel)
      (msg match {
        case Left(str)    => mb.withContent(str)
        case Right(embed) => mb.withEmbed(embed)
      }).build

    case EnableBot(channel) =>
      if (!subscribedChannels.contains(channel)) {
        self ! SendMessage(Left("You have entered East Commonlands."), channel)
        context.become(running(client, subscribedChannels + channel, auctions))
      }

    case DisableBot(channel) =>
      if (subscribedChannels.contains(channel)) {
        self ! SendMessage(Left("Steps into the shadows and fades away."), channel)
        context.become(running(client, subscribedChannels - channel, auctions))
      }

    case pa: PostAuction =>
      if (auctions.size == MaxAuctionsPerMessage) {
        writeAuctions(client, subscribedChannels, auctions)
        context.become(running(client, subscribedChannels, List(pa)))
      } else {
        context.become(running(client, subscribedChannels, pa +: auctions))
      }

    case WriteAuctions =>
      if (auctions.nonEmpty) {
        writeAuctions(client, subscribedChannels, auctions)
        context.become(running(client, subscribedChannels, Nil))
      }
  }

}

case class SendMessage(msg: Either[String, EmbedObject], channel: IChannel)

case object WriteAuctions
