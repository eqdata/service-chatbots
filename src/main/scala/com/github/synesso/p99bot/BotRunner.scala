package com.github.synesso.p99bot

import akka.actor.{ActorRef, ActorSystem, Props}
import io.scalac.slack.MessageEventBus
import io.scalac.slack.api._
import io.scalac.slack.common._
import io.scalac.slack.common.actors._
import io.scalac.slack.websockets.WebSocket

object BotRunner extends Shutdownable {
  val system = ActorSystem("SlackBotSystem")
  val eventBus = new MessageEventBus
  val slackBot: ActorRef = system.actorOf(Props(classOf[SlackBotActor], new BotsBundle(eventBus), eventBus, this, None), "slack-bot")
  var botInfo: Option[BotInfo] = None

  def main(args: Array[String]) {
    try {
      slackBot ! Start
      system.awaitTermination()
    } catch {
      case e: Exception =>
        system.shutdown()
        system.awaitTermination()
    }
  }

  sys.addShutdownHook(shutdown())

  override def shutdown(): Unit = {
    slackBot ! WebSocket.Release
    system.shutdown()
    system.awaitTermination()
  }
}
