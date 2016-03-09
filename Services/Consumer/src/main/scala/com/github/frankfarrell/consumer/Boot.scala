package com.github.frankfarrell.consumer

import java.net.InetSocketAddress

import akka.actor.ActorSystem

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import eventstore.tcp.ConnectionActor
import eventstore.{EsConnection, UserCredentials, Settings, EventStoreExtension}

import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("consumer-system")

  implicit val materializer = ActorMaterializer()

  val settings = Settings(
    address = new InetSocketAddress("127.0.0.1", 1113),
    defaultCredentials = Some(UserCredentials("admin", "changeit")))

  val connection = EsConnection(system, settings)

  val publisher = connection.allStreamsPublisher()

  Source.fromPublisher(publisher)
    .groupedWithin(Int.MaxValue, 1.second)
    .runForeach { xs => println(f"${xs.size.toDouble}%2.1f m/s") }
}