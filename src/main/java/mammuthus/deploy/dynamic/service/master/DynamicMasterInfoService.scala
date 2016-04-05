package mammuthus.deploy.dynamic.service.master

import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import com.google.inject.Singleton
import mammuthus.deploy.dynamic.protocol.InstallPhase
import mammuthus.deploy.dynamic.{DynamicDeployMaster, ShutdownHookManager}
import net.csdn.common.logging.Loggers

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * 10/25/15 WilliamZhu(allwefantasy@gmail.com)
 */
@Singleton
class DynamicMasterInfoService {
  val logger = Loggers.getLogger(getClass)
  val yarnContainers = new ConcurrentHashMap[String, Long]()
  val appContainers = new ConcurrentHashMap[String, String]()
  val containerToDockerContainer = new ConcurrentHashMap[String, String]()
  val containerToDockerInstalling = new ConcurrentHashMap[String, InstallPhase]()
  val offlineContainers = new ArrayBuffer[String]()

  val failContainers = new ConcurrentHashMap[String, String]()

  var imageUrl: String = ""
  var startCommand: String = ""
  var location: String = ""

  val containerCleanThread = Executors.newSingleThreadScheduledExecutor()

  containerCleanThread.scheduleAtFixedRate(new Runnable {
    override def run(): Unit = {
      yarnContainers.filter(f => System.currentTimeMillis() - f._2 > 60 * 1000 && !offlineContainers.contains(f._1)).foreach { f =>
        offlineContainers.add(f._1)
      }
      offlineContainers.foreach { f =>
        yarnContainers.remove(f)
        cleanCrackedContainer(f)
      }
      offlineContainers.clear()

    }
  }, 1000, 10000, TimeUnit.MILLISECONDS)

  ShutdownHookManager.get().addShutdownHook(new Runnable {
    override def run(): Unit = {
      logger.error("Driver be killed,unregister apps from Nginx")
      appContainers.foreach { k =>
        logger.error(s"unregister ${k._2} from ${k._1}")
        DynamicDeployMaster.mammuthusMasterClient.removeServersFromUpstream(k._2, k._1)
      }

    }
  }, 1)


  //prevent docker container leak
  def cleanCrackedContainer(hostAndPort: String) = {
    if (containerToDockerContainer.containsKey(hostAndPort)) {
      val dockerContainerId = containerToDockerContainer(hostAndPort)
      logger.info(s"$hostAndPort cracked unexepected. " +
        s"we should clean docker container [${dockerContainerId}}] managed by this instance.")
      DynamicDeployMaster.mammuthusMasterClient.stopContainer(
        hostAndPort.split(":").head
        , dockerContainerId)
    }
  }
}
