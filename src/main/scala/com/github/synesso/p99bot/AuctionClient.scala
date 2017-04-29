package com.github.synesso.p99bot

import akka.actor.ActorRef
import io.socket.client.{IO, Socket}
import io.socket.emitter.Emitter
import org.json.{JSONArray, JSONObject}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsonFormat, _}

import scala.collection.mutable.ListBuffer
import scala.util.Random

object AuctionClient {}
