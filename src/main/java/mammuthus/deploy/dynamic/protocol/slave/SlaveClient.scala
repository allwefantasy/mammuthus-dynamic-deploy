package mammuthus.deploy.dynamic.protocol.slave

import net.csdn.annotation.Param
import net.csdn.annotation.rest.{At, BasicInfo, State}
import net.csdn.modules.http.RestRequest
import net.csdn.modules.transport.HttpTransportService
import net.liftweb.{json => SJSon}

import scala.collection.JavaConversions._

/**
 * 10/27/15 WilliamZhu(allwefantasy@gmail.com)
 */
trait SlaveClient {
  @At(path = Array("/application/start"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?imageUrl=&startCommand=&host=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def startContainer(@Param("imageUrl") imageUrl: String,
                     @Param("location") location: String,
                     @Param("startCommand") startCommand: String,
                     @Param("phase") phase: String): java.util.List[HttpTransportService.SResponse]
}

case class StartContainerResponse(success: Boolean, id: String, host: String)

class SResponseEnhancer(x: java.util.List[HttpTransportService.SResponse]) {
  def startContainerResult: Boolean = {
    if (x == null || x.isEmpty || x(0).getStatus != 200) {
      return false
    }
    return true
  }

  private def extract[T]()(implicit manifest: Manifest[T]): T = {
    if (x == null || x.isEmpty || x(0).getStatus != 200) {
      return null.asInstanceOf[T]
    }
    implicit val formats = SJSon.DefaultFormats
    SJSon.parse(x(0).getContent).extract[T]
  }
}

object SResponseEnhancer {
  implicit def mapSResponseToObject(x: java.util.List[HttpTransportService.SResponse]): SResponseEnhancer = {
    new SResponseEnhancer(x)
  }
}

