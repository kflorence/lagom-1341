package com.example.hello.impl

import java.util.UUID

import akka.Done
import slick.dbio.Effect
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global

case class GreetingSnapshot(id: UUID, greeting: String)

class GreetingRepository {
  class GreetingTable(tag: Tag) extends Table[GreetingSnapshot](tag, "greeting") {
    def id = column[UUID]("id", O.PrimaryKey)
    def greeting = column[String]("greeting")

    def * = (id, greeting) <> (GreetingSnapshot.tupled, GreetingSnapshot.unapply)
  }

  val snapshots = TableQuery[GreetingTable]

  def createTableIfNotExisting: DBIOAction[Unit, NoStream, Effect.Read with Effect.Schema with Effect.Transactional] =
    MTable
      .getTables("%")
      .flatMap { tables =>
        if (!tables.exists(_.name.name == snapshots.baseTableRow.tableName)) {
          snapshots.schema.create
        } else {
          DBIO.successful((): Unit)
        }
      }
      .transactionally

  def save(snapshot: GreetingSnapshot): DBIOAction[Done.type, NoStream, Effect.Write] = {
    snapshots.insertOrUpdate(snapshot).map(_ => Done)
  }
}
