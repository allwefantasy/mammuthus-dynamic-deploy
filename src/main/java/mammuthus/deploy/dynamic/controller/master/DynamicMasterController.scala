package mammuthus.deploy.dynamic.controller.master

import java.util

import com.google.inject.Inject
import mammuthus.deploy.dynamic.DynamicDeployMaster
import mammuthus.deploy.dynamic.protocol.InstallPhase
import mammuthus.deploy.dynamic.protocol.slave.SResponseEnhancer._
import mammuthus.deploy.dynamic.protocol.slave.SlaveClient
import mammuthus.deploy.dynamic.service.master.DynamicMasterInfoService
import net.csdn.annotation.rest.{At, BasicInfo, State}
import net.csdn.modules.http.{ApplicationController, RestRequest, ViewType}
import net.csdn.modules.transport.HttpTransportService
import net.csdn.modules.transport.proxy.{AggregateRestClient, FirstMeetProxyStrategy}
import net.liftweb.{json => SJSon}
import net.sf.json.JSONObject

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * 7/17/15 WilliamZhu(allwefantasy@gmail.com)
 */
class DynamicMasterController @Inject()(
                                         dynamicMasterInfoService: DynamicMasterInfoService,
                                         transportService: HttpTransportService) extends ApplicationController {
  implicit val formats = SJSon.Serialization.formats(SJSon.NoTypeHints)


  @At(path = Array("/"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def firstPage = {
    redirectTo("/containers", params())
  }

  @At(path = Array("/slave/heart/beat"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?host=&port=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def register = {
    val hostAndPort = param("host") + ":" + param("port")

    if (!dynamicMasterInfoService.yarnContainers.containsKey(hostAndPort)) {
      slaveClient(hostAndPort).startContainer(
        dynamicMasterInfoService.imageUrl,
        dynamicMasterInfoService.location,
        dynamicMasterInfoService.startCommand,
        "all").startContainerResult
    }
    dynamicMasterInfoService.yarnContainers.put(hostAndPort, System.currentTimeMillis())
    render(SJSon.Serialization.write(Map()), ViewType.json)
  }

  @At(path = Array("/container/install/phase"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?hostAndPort=&status=&image=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def containerInstallPhase = {
    logger.info(s"containerInstallPhase ${param("hostAndPort")} => ${param("image")} ${param("status")}")
    dynamicMasterInfoService.containerToDockerInstalling.put(
      param("hostAndPort"),
      InstallPhase(param("image"), param("status"), System.currentTimeMillis()))
    if (param("status").startsWith("run:success")) {
      val dockerContainerId = param("status").split(":").last
      dynamicMasterInfoService.containerToDockerContainer.put(param("hostAndPort"), dockerContainerId)
    }
    if (param("status").startsWith("exit-")) {
      removeServersFromUpstream
    }
    if (param("status").startsWith("start-")) {
      addServersToUpstream
    }
    render(200, "")
  }

  private def addServersToUpstream = {
    val hp = param("status").split("-").last
    val fileName = dynamicMasterInfoService.startCommand.split("\\s+")(1)
    val name = fileName.split("_")(1).replaceAll("\\.sh$", "")
    logger.info(s"register $hp to $name in nginx")
    val res = DynamicDeployMaster.mammuthusMasterClient.addServersToUpstream(name, hp)

    if (res == null || res.isEmpty || res(0).getStatus != 200) {
      logger.error(s"fail to register $hp to $name in nginx")
    } else {
      dynamicMasterInfoService.appContainers.put(hp, name)
    }

  }


  private def removeServersFromUpstream = {


    val hp = param("status").split("-").last

    dynamicMasterInfoService.appContainers.remove(hp)
    dynamicMasterInfoService.containerToDockerContainer.remove(param("hostAndPort"))
    dynamicMasterInfoService.failContainers.put(param("hostAndPort"), hp)

    val fileName = dynamicMasterInfoService.startCommand.split("\\s+")(1)
    val name = fileName.split("_")(1).replaceAll("\\.sh$", "")
    logger.info(s"remove $hp from $name in nginx")
    val res = DynamicDeployMaster.mammuthusMasterClient.removeServersFromUpstream(name, hp)
    if (res == null || res.isEmpty || res(0).getStatus != 200) {
      logger.error(s"fail to remove $hp from $name in nginx")
    } else {
      dynamicMasterInfoService.appContainers.remove(hp)
    }
  }

  @At(path = Array("/containers"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?host=&port=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def containers = {
    val items = new util.HashMap[String, Any]()
    import scala.collection.JavaConversions._
    dynamicMasterInfoService.containerToDockerInstalling.foreach(f => items.put(f._1, f._2.toString()))
    val res = new util.HashMap[String, Any]()
    res.put("dynamicMasterInfoService.containerToDockerContainer", dynamicMasterInfoService.containerToDockerContainer)
    res.put("dynamicMasterInfoService.containerToDockerInstalling", items)
    res.put("dynamicMasterInfoService.yarnContainers", dynamicMasterInfoService.yarnContainers)
    res.put("dynamicMasterInfoService.appContainers", dynamicMasterInfoService.appContainers)
    res.put("dynamicMasterInfoService.failContainers", dynamicMasterInfoService.failContainers)
    render(200, JSONObject.fromObject(res).toString())
  }

  @At(path = Array("/shutdown"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def shutdown = {
    val items = new ArrayBuffer[String]()
    dynamicMasterInfoService.appContainers.foreach { f =>

      val res = DynamicDeployMaster.mammuthusMasterClient.removeServersFromUpstream(f._2, f._1)
      if (res == null || res.size() == 0) {
        items += f._1
      } else {
        if (res.get(0).getStatus != 200)
          items += f._1
      }
    }
    //http://dm-dev-01:9003/core/application/kill?appId=application_1446452916390_0015
    DynamicDeployMaster.mammuthusMasterClient.killApplication(param("appId"))
    render(200, SJSon.Serialization.write(items))
  }


  def slaveClient(hostAndPort: String) = {
    AggregateRestClient.buildIfPresent[SlaveClient](hostAndPort, new FirstMeetProxyStrategy, transportService)
  }
}
