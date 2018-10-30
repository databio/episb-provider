import com.github.oddodaoddo.episb._
import org.scalatra._
import javax.servlet.ServletContext
import org.elasticsearch.client.transport.TransportClient

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val esclient: Either[String,TransportClient] = ElasticConnector.getESClient("localhost", 9300)
    esclient match {
      case Right(es) => {
        context.mount(new episbRestServlet(es), "/*")
      }
      case Left(msg) => println(msg)
    }
  }
}
