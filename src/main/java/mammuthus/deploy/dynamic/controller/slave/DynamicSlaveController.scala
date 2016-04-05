package mammuthus.deploy.dynamic.controller.slave

import mammuthus.deploy.dynamic.service.slave.container.{DockerContainer, JavaContainer}
import net.csdn.annotation.rest.{At, BasicInfo, State}
import net.csdn.modules.http.{ApplicationController, RestRequest}
import net.liftweb.{json => SJSon}

/**
 * 7/17/15 WilliamZhu(allwefantasy@gmail.com)
 */
class DynamicSlaveController extends ApplicationController {
  implicit val formats = SJSon.Serialization.formats(SJSon.NoTypeHints)


  @At(path = Array("/application/start"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?imageUrl=&startCommand=&location=&phase=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def applicationStart = {
    val container = if (param("imageUrl").endsWith(".tar.gz"))
      findService(classOf[JavaContainer])
    else findService(classOf[DockerContainer])

    container.runContainer(
      param("imageUrl"),
      param("location"),
      param("startCommand"),
      param("phase"))

    render(200, "")
  }
}
