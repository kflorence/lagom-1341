package com.example.hello.impl

import com.dimafeng.testcontainers.{ForAllTestContainer, MySQLContainer}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.example.hello.api._
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import play.api.Configuration

class HellolagomServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer {
  val container = MySQLContainer()

  private lazy val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withJdbc()
  ) { ctx =>
    new HellolagomApplication(ctx) with LocalServiceLocator {
      override def additionalConfiguration: AdditionalConfiguration = {
        super.additionalConfiguration ++ Configuration(
          "db.default.url" -> (container.jdbcUrl + "?useSSL=true&verifyServerCertificate=false"),
          "db.default.username" -> container.username,
          "db.default.password" -> container.password
        ).underlying
      }
    }
  }

  private lazy val client = server.serviceClient.implement[HellolagomService]

  override protected def afterAll() = server.stop()

  "hello-lagom service" should {

    "say hello" in {
      client.hello("Alice").invoke().map { answer =>
        answer should ===("Hello, Alice!")
      }
    }

    "allow responding with a custom message" in {
      for {
        _ <- client.useGreeting("Bob").invoke(GreetingMessage("Hi"))
        answer <- client.hello("Bob").invoke()
      } yield {
        answer should ===("Hi, Bob!")
      }
    }
  }
}
