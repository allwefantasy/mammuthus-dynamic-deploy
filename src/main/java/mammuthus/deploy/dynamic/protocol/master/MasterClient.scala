package mammuthus.deploy.dynamic.protocol.master

import mammuthus.deploy.dynamic.protocol.HeartBeat
import net.csdn.annotation.Param
import net.csdn.annotation.rest.{At, BasicInfo, State}
import net.csdn.modules.http.RestRequest
import net.csdn.modules.transport.HttpTransportService
import net.csdn.modules.transport.HttpTransportService.SResponse
import net.liftweb.{json => SJSon}

import scala.collection.JavaConversions._

/**
 * 10/26/15 WilliamZhu(allwefantasy@gmail.com)
 */
trait MasterClient {
  @At(path = Array("/slave/heart/beat"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?host=&port=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def heartbeat(@Param("host") host: String, @Param("port") port: String): java.util.List[SResponse]

  @At(path = Array("/container/install/phase"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?hostAndPort=&status=&image=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def containerInstallPhase(@Param("hostAndPort") hostAndPort: String, @Param("image") image: String, @Param("status") status: String): java.util.List[SResponse]
}

class SResponseEnhancer(x: java.util.List[HttpTransportService.SResponse]) {
  def heartbeatResult: HeartBeat = {
    extract[HeartBeat]()
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
