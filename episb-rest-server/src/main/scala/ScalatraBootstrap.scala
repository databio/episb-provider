import com.github.oddodaoddo.episb._
import org.scalatra._
import javax.servlet.ServletContext
import org.elasticsearch.client.transport.TransportClient

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new episbRestServlet, "/*")
  }
}
