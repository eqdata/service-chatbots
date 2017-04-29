package com.github.eqdata

import akka.actor.{Actor, ActorSystem}
import com.github.eqdata.AuctionAgent.AuctionUpdate
import slack.api.SlackApiClient

class SlackPublisher(token: String) extends Actor {

  implicit val system: ActorSystem = context.system

  val client = SlackApiClient(token)

  override def receive: Receive = {
    case update: AuctionUpdate =>
      // todo - lookup channel id
      // todo - why doesn't shady's avatar show?
      // todo - migrate glomeration from old "AuctionCommands" class
      client.postChatMessage("G4Q22VA67", update.auctions.map(_.line).mkString("\n"))
  }
}
