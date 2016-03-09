package com.github.frankfarrell.server

import java.net.InetSocketAddress

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, Actor, Props, ActorSystem}
import akka.io.IO
import eventstore.{WriteEventsCompleted, Settings, UserCredentials,EsException}

import spray.can.Http

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("event-system")

  // create and start our service actor
  val service = system.actorOf(Props[EventServer], "event-service")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)

}