package com.github.eqdata

import akka.actor.{Actor, ActorSystem}
import com.github.eqdata.AuctionAgent.{AuctionUpdate, Item, User}
import slack.api.SlackApiClient

class SlackPublisher(token: String) extends Actor {

  implicit val system: ActorSystem = context.system

  val client = SlackApiClient(token)

  override def receive: Receive = {
    // todo - lookup channel id from configured name via api
    // todo - why doesn't shady's avatar show?
    case (User(name), items: Set[Item]) =>
      val msg = items
        .map { i =>
          s"<https://wiki.project1999.com/${i.uri}|${i.name}>"
        }
        .toList
        .sorted
        .mkString(s"$name is selling ", ", ", ".")
      client.postChatMessage("G4Q22VA67", msg)
  }
}
