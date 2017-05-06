package com.github.eqdata

import akka.actor.{ActorSystem, Props}
import com.github.eqdata.AuctionAgent.Subscribe
import com.typesafe.config.ConfigFactory

object Main extends App {

  implicit val system = ActorSystem("eqdata")

  private val config = ConfigFactory.load

  val auctioneer = {
    val webSocketUrl = config.getString("auctioneer.websocket-url")
    val serverType = config.getString("auctioneer.server-type")
    val auctionAgentProps = Props(new AuctionAgent(webSocketUrl, serverType))
    system.actorOf(auctionAgentProps, "auctioneer")
  }

  val slack = {
    val token = config.getString("slack.token")
    val channel = config.getString("slack.channel")
    system.actorOf(Props(new SlackPublisher(token, channel)), "slack-publisher")
  }

  auctioneer ! Subscribe(slack)
  auctioneer ! Start

}

case object Start
case object Stop
