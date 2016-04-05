package mammuthus.deploy.dynamic


import java.net.{URI, InetAddress}
import java.util

import mammuthus.controller.master.api.TaskService
import mammuthus.deploy.dynamic.service.master.DynamicMasterInfoService
import mammuthus.yarn.MammuthusApplication
import net.csdn.ServiceFramwork
import net.csdn.bootstrap.Application
import net.csdn.modules.http.HttpServer
import net.csdn.modules.transport.HttpTransportService
import net.csdn.modules.transport.proxy.{FirstMeetProxyStrategy, AggregateRestClient}


/**
 * 8/14/15 WilliamZhu(allwefantasy@gmail.com)
 */

object KK {
  def main(args: Array[String]): Unit = {
    val th = new Thread(new Runnable {
      override def run(): Unit = new DynamicDeployMaster().run(Array("1", "2", "3"), new util.HashMap[String, String]())

      println("yes")
      Thread.currentThread().join()
    })
    th.setDaemon(true)
    th.start()

  }
}

object DynamicDeployMaster {
  var  mammuthusMasterClient:TaskService = _
  def main(args: Array[String]): Unit = {
    ServiceFramwork.scanService.setLoader(classOf[DynamicDeployMaster])
    ServiceFramwork.applicaionYamlName("application.master.yml")
    ServiceFramwork.disableDubbo()
    ServiceFramwork.enableNoThreadJoin()
    Application.main(args)
  }

}

class DynamicDeployMaster extends MammuthusApplication {

  var httpPort: Int = 0
  var _uiAddress = ""
  var mammuthusMasterAddress:String = _


  override def masterPort: Int = httpPort

  override def uiAddress: String = _uiAddress

  override def run(args: Array[String], maps: java.util.Map[String, String]): Unit = {
    DynamicDeployMaster.main(args)
    val hp = ServiceFramwork.injector.getInstance(classOf[HttpServer])
    httpPort = hp.getHttpPort
    val containerService = ServiceFramwork.injector.getInstance(classOf[DynamicMasterInfoService])
    containerService.imageUrl = args(0)
    containerService.location = args(1)
    containerService.startCommand = args(2)
    mammuthusMasterAddress = args(3)
    maps.put("httpPort", masterPort + "")
    maps.put("uiAddress", s"http://${InetAddress.getLocalHost.getHostName}:${masterPort}")
    _uiAddress = maps.get("uiAddress")
    DynamicDeployMaster.mammuthusMasterClient = AggregateRestClient.buildClient[TaskService](List(
      driverHostAndPort
    ), new FirstMeetProxyStrategy(), ServiceFramwork.injector.getInstance(classOf[HttpTransportService]))

    Thread.currentThread().join()
  }



  private def driverHostAndPort = {
    val uri = URI.create(mammuthusMasterAddress)
    uri.getHost + ":" + uri.getPort
  }
}
