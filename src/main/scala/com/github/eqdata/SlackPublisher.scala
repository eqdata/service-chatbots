package com.github.eqdata

import akka.actor.{Actor, ActorSystem}
import com.github.eqdata.AuctionAgent.{AuctionUpdate, Item, User}
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigException.BadValue
import slack.api.SlackApiClient
import slack.models.Channel
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SlackPublisher(token: String, channel: String) extends Actor {

  implicit val system: ActorSystem = context.system

  val client = SlackApiClient(token)

  val channelLookup: Future[Channel] = client
    .listChannels()
    .map(
      _.find(_.name == channel)
        .getOrElse(throw new BadValue("slack.channel", "no channel with this name")))

  override def receive: Receive = {
    // todo - why doesn't shady's avatar show?
    case (User(name), items: Set[Item]) =>
      for {
        channel <- channelLookup
        msg = items
          .map { i =>
            s"<https://wiki.project1999.com/${i.uri}|${i.name}>"
          }
          .toList
          .sorted
          .mkString(s"$name is selling ", ", ", ".")
      } client.postChatMessage(channel.id, msg)
  }
}
