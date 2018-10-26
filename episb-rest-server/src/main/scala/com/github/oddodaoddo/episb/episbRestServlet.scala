package com.github.oddodaoddo.episb

import org.scalatra._

class episbRestServlet extends ScalatraServlet {

  get("/") {
    views.html.hello()
  }

}
