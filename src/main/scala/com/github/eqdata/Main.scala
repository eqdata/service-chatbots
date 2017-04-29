package com.github.eqdata

import akka.actor.{ActorSystem, Props}
import com.github.eqdata.AuctionAgent.{Start, Subscribe}
import com.typesafe.config.ConfigFactory

object Main extends App {

  implicit val system = ActorSystem("eqdata")

  private val config = ConfigFactory.load

  val auctioneer = system.actorOf(
    Props(
      new AuctionAgent(
        webSocketUrl = config.getString("auctioneer.websocket-url"),
        serverType = config.getString("auctioneer.server-type")
      )))

  val slack = system.actorOf(Props(new SlackPublisher(config.getString("slack.token"))), "slack-publisher")

  auctioneer ! Subscribe(slack)
  auctioneer ! Start

}
