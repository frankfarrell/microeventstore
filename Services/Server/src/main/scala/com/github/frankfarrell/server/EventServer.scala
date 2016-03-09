package com.github.frankfarrell.server

import java.net.InetSocketAddress

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, Props, Actor}
import eventstore._
import eventstore.tcp.ConnectionActor
import spray.httpx.SprayJsonSupport
import spray.routing.HttpService
import spray.json._
import spray.httpx.SprayJsonSupport._

/**
  * Created by Frank on 08/03/2016.
  *
  * Post the following to {"orgName":"FrankOrg","appName":"FrankApp","duration":264} to localhost:8080
  * persists this event in event Store
  *
  * To read event at index 0
  * curl -i http://127.0.0.1:2113/streams/my-stream/0 -H "Accept: application/json"
  */

//The order of declarations matter for implicit values
final case class BillingEvent(orgName : String, appName: String, duration : Long)

// collect your json format instances into a support trait:
object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val billingEventFormat : RootJsonFormat[BillingEvent] = jsonFormat3(BillingEvent)
}

class WriteResult(name : String) extends Actor with ActorLogging {
  def receive = {
    case WriteEventsCompleted(range, position) =>
      log.info("range: {}, position: {}", range, position)
    case Failure(e: EsException) =>
      log.error(e.toString)
  }
}

class EventServer extends Actor with HttpService{

  def actorRefFactory = context

  def receive = runRoute(postRoute)

  val settings = Settings(
    address = new InetSocketAddress("127.0.0.1", 1113),
    defaultCredentials = Some(UserCredentials("admin", "changeit")))

  val connection = context.actorOf(ConnectionActor.props(settings), "connection-actor")

  implicit val writeResult = context.actorOf(Props(classOf[WriteResult], "blah"))

  val postRoute ={

    import JsonSupport._

    path(""){
      post {
        entity(as[BillingEvent]) { billing => // will unmarshal JSON to Order
          val event = EventData("billing-event",
            data = Content(billingEventFormat.write(billing).toString()))

          connection ! WriteEvents(EventStream.Id("my-stream"), List(event))

          complete {
            <html>
              <body>
                <h1>Event Persisted</h1>
              </body>
            </html>
          }
        }
      }
    }
  }

}
