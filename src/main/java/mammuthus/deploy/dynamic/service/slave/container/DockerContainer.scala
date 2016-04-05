package mammuthus.deploy.dynamic.service.slave.container

import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

import com.google.inject.Inject
import mammuthus.deploy.dynamic.service.slave._
import mammuthus.deploy.dynamic.service.slave.monitor.ContainerLivelinessMonitor
import mammuthus.deploy.dynamic.service.slave.tool.{ContainerTool, LoadImageTool}
import mammuthus.deploy.dynamic.{DynamicDeploySlave, ShutdownHookManager}
import net.csdn.common.logging.Loggers
import net.csdn.common.settings.Settings
import net.csdn.modules.threadpool.ThreadPoolService

/**
 * 10/28/15 WilliamZhu(allwefantasy@gmail.com)
 */
@Inject
class DockerContainer @Inject()(heartBeatService: HeartBeatService,
                                containerLivelinessMonitor: ContainerLivelinessMonitor,
                                threadPool: ThreadPoolService,settings:Settings) extends Container {
  val logger = Loggers.getLogger(getClass)

  def APP_LOCATION:String = settings.get("application.install.location","/tmp")

  override def containerHostAndPort = heartBeatService.containerHostAndPort

  override def timeout = 5 * 1000 * 60l

  var dockerContainerId: AtomicReference[String] = new AtomicReference[String]()
  var dockerContainerPort: String = _

  containerLivelinessMonitor.monitor(dockerContainerId, (containerId) => {
    val res = execWithUserAndExitValue("", s"docker inspect -f {{.State.Running}} $dockerContainerId", 5000)
    res.out.toBoolean
  }, (containerId) => {
    sendExitMessageToAM
    System.exit(-1)
  })


  override def _load(appLocation:String,imageUrl: String, location: String, startCommand: String) = {
    val loadRes = LoadImageTool.loadImage(location, timeout)
    execWithUserAndExitValue("", s"rm -f ${location}", timeout)
    if (loadRes.exitValue != 0) {
      sendMessage(imageUrl, "load:" + loadRes.err)
      (false, "")
    } else {
      sendMessage(imageUrl, "load:success")
      (true, "")
    }

  }

  override def _run(imageUrl: String, location: String, startCommand: String) = {

    dockerContainerPort = new RandomPortGenerator().getPort + ""

    val newCommand = startCommand.replaceAll("\\$\\{ramdom_port\\}", dockerContainerPort)
      .replaceAll("\\$\\{memory\\}", "--memory " + DynamicDeploySlave.applicationMasterArguments.executeMemory + "m")
      .replaceAll("\\$\\{cores\\}", "--cpu " + DynamicDeploySlave.applicationMasterArguments.cores)


    val runRes = ContainerTool.runContainer(newCommand, timeout)
    if (runRes.exitValue != 0) {
      sendMessage(imageUrl, "run:" + runRes.err)
      (false, "")
    } else {
      sendMessage(imageUrl, s"run:success:${runRes.out}")
      sendStartMessageToAM
      // we can shutdown docker container gracefully when Yarn Container killed
      // however,when JVM cracks unexpected, AM will handle this instead of ShutdownHookManager
      ShutdownHookManager.get().addShutdownHook(new Runnable {
        override def run(): Unit = {
          execWithUserAndExitValue("", s"docker stop ${runRes.out}", 100 * 1000)
          sendExitMessageToAM
        }
      }, 1)
      dockerContainerId.set(runRes.out)
      (true, "")
    }
  }

  def sendExitMessageToAM = {
    sendMessage("-", "exit-" + InetAddress.getLocalHost.getHostAddress + ":" + dockerContainerPort)
  }

  def sendStartMessageToAM = {
    sendMessage("-", "start-" + InetAddress.getLocalHost.getHostAddress + ":" + dockerContainerPort)
  }

}
