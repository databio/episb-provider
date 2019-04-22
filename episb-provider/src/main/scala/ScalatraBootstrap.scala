import com.github.oddodaoddo.episb._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new EpisbSwagger

  override def init(context: ServletContext) {

    context.initParameters("org.scalatra.cors.allowedOrigins") = "http://episb.swagger.io"

    context.mount(new episbRestServlet, "/*")
    context.mount (new ResourcesApp, "/api-docs")
  }
}
