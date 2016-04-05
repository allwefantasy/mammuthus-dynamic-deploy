package mammuthus.deploy.dynamic.service.slave

import java.net.InetAddress
import java.util.concurrent.{Executors, TimeUnit}

import com.google.inject.{Inject, Singleton}
import net.csdn.common.settings.Settings
import net.csdn.modules.http.HttpServer

/**
 * 10/25/15 WilliamZhu(allwefantasy@gmail.com)
 */
@Singleton
class HeartBeatService @Inject()(settings: Settings, httpServer: HttpServer) {
  val heartBeat = Executors.newSingleThreadScheduledExecutor()
  heartBeat.scheduleAtFixedRate(new Runnable {
    override def run(): Unit = {
      MasterClient.masterClient.heartbeat(InetAddress.getLocalHost.getHostName, httpServer.getHttpPort + "")
    }
  }, 1000, 5000, TimeUnit.MILLISECONDS)

  def containerHostAndPort = {
    InetAddress.getLocalHost.getHostName + ":" + httpServer.getHttpPort
  }
}
