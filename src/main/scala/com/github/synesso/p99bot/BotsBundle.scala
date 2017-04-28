package com.github.synesso.p99bot

import akka.actor.{ActorContext, ActorRef, Props}
import io.scalac.slack.{BotModules, MessageEventBus}
import io.scalac.slack.bots.system.{CommandsRecognizerBot, HelpBot}

class BotsBundle(eventBus: MessageEventBus) extends BotModules {
  override def registerModules(context: ActorContext, websocketClient: ActorRef): Unit = {
    context.actorOf(Props(classOf[CommandsRecognizerBot], eventBus), "commandProcessor")
    context.actorOf(Props(classOf[HelpBot], eventBus), "helpBot")
    context.actorOf(Props(classOf[AuctionCommands], eventBus), "AuctionCommands")
  }
}
