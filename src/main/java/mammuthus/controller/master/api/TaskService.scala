package mammuthus.controller.master.api

import net.csdn.annotation.Param
import net.csdn.annotation.rest.{At, BasicInfo, State}
import net.csdn.modules.http.RestRequest
import net.csdn.modules.transport.HttpTransportService

/**
 * 4/2/16 WilliamZhu(allwefantasy@gmail.com)
 */
trait TaskService {
  @At(path = Array("/core/application/kill"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def killApplication(@Param("appId") appId: String): java.util.List[HttpTransportService.SResponse]

  @At(path = Array("/add/servers/to/upstream"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?name=&ips=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def addServersToUpstream(@Param("name") name: String, @Param("ips") ips: String): java.util.List[HttpTransportService.SResponse]


  @At(path = Array("/remove/servers/from/upstream"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?name=&ips=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def removeServersFromUpstream(@Param("name") name: String, @Param("ips") ips: String): java.util.List[HttpTransportService.SResponse]

  @At(path = Array("/stop/container"), types = Array(RestRequest.Method.GET, RestRequest.Method.POST))
  @BasicInfo(
    desc = "",
    state = State.alpha,
    testParams = "?name=&ips=",
    testResult = "",
    author = "WilliamZhu",
    email = "allwefantasy@gmail.com"
  )
  def stopContainer(@Param("hostAndPort") hostAndPort: String, @Param("id") id: String): java.util.List[HttpTransportService.SResponse]
}
