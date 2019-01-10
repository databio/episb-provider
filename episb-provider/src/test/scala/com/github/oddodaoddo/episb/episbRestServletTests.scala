package com.github.oddodaoddo.episb

import org.scalatra.test.scalatest._

class episbRestServletTests extends ScalatraFunSuite {

  addServlet(classOf[episbRestServlet], "/*")

  test("GET / on episbRestServlet should return status 200") {
    get("/") {
      status should equal (200)
    }
  }

}
