package mammuthus.deploy.dynamic.service.slave

import java.net.URI

import mammuthus.deploy.dynamic.DynamicDeploySlave
import mammuthus.deploy.dynamic.protocol.master.MasterClient
import net.csdn.modules.transport.proxy.{AggregateRestClient, FirstMeetProxyStrategy}

/**
 * 10/28/15 WilliamZhu(allwefantasy@gmail.com)
 */

object MasterClient {
  val masterClient = AggregateRestClient.buildClient[MasterClient](List(
    driverHostAndPort
  ), new FirstMeetProxyStrategy(), AggregateRestClient.buildTransportService)

  private def driverHostAndPort = {
    val uri = URI.create(DynamicDeploySlave.applicationMasterArguments.driverUrl)
    uri.getHost + ":" + uri.getPort
  }
}
