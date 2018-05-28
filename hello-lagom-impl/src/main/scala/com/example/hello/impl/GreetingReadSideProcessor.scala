package com.example.hello.impl

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import slick.dbio.Effect
import slick.jdbc.MySQLProfile

class GreetingReadSideProcessor(
  readSide: SlickReadSide,
  greetingRepository: GreetingRepository
) extends ReadSideProcessor[HellolagomEvent] {

  def aggregateTags: Set[AggregateEventTag[HellolagomEvent]] = Set(HellolagomEvent.Tag)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HellolagomEvent] =
    readSide
      .builder[HellolagomEvent]("greeting_offset")
      .setGlobalPrepare(greetingRepository.createTableIfNotExisting)
      .setEventHandler[GreetingMessageChanged](processEvent)
      .build()

  protected[impl] def processEvent(element: EventStreamElement[GreetingMessageChanged]): MySQLProfile.api.DBIOAction[Done.type, MySQLProfile.api.NoStream, Effect.Write] = {
    val snapshot = GreetingSnapshot(UUID.randomUUID, element.event.message)
    greetingRepository.save(snapshot)
  }
}