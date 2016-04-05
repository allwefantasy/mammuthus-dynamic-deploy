package mammuthus.deploy.dynamic.service.slave.container

import java.net.InetAddress
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicReference

import com.google.common.io.Files
import com.google.inject.{Inject, Singleton}
import mammuthus.deploy.dynamic.service.slave.monitor.ContainerLivelinessMonitor
import mammuthus.deploy.dynamic.service.slave.tool.LoadImageTool
import mammuthus.deploy.dynamic.service.slave.{HeartBeatService, RandomPortGenerator}
import mammuthus.deploy.dynamic.{DynamicDeploySlave, ShutdownHookManager}
import net.csdn.common.logging.Loggers
import net.csdn.common.settings.Settings
import net.csdn.modules.threadpool.ThreadPoolService

/**
 * 10/28/15 WilliamZhu(allwefantasy@gmail.com)
 */
@Singleton
class JavaContainer @Inject()(heartBeatService: HeartBeatService,
                              containerLivelinessMonitor: ContainerLivelinessMonitor,
                              threadPool: ThreadPoolService, settings: Settings) extends Container {


  val logger = Loggers.getLogger(getClass)

  def APP_LOCATION: String = settings.get("application.install.location", "/tmp")

  var javaContainerId: AtomicReference[String] = new AtomicReference[String]()
  var javaContainerPort: String = _

  override def containerHostAndPort = heartBeatService.containerHostAndPort

  override def timeout = 5 * 1000 * 60l

  containerLivelinessMonitor.monitor(javaContainerId, (containerId) => {
    val res = execWithUserAndExitValue("", s"ps -p $javaContainerId", 5000)
    res.exitValue == 0
  }, (containerId) => {
    exitMessageToAM
    System.exit(-1)
  })


  override def _load(appLocation: String, imageUrl: String, location: String, startCommand: String) = {
    val (_appDir, loadRes) = LoadImageTool.loadTarGz(appLocation, location, timeout)
    execWithUserAndExitValue("", s"rm -f ${location};", timeout)
    if (loadRes.exitValue != 0) {
      sendMessage(imageUrl, "load:" + loadRes.err)
      (false, _appDir)
    } else {
      sendMessage(imageUrl, "load:success")
      (true, _appDir)
    }

  }


  override def _run(imageUrl: String, dynamicAppDir: String, startCommand: String): (Boolean, String) = {

    val appDir = dynamicAppDir + "/" + imageUrl.split("/").last.split("@")(0)

    if (!appDir.startsWith(APP_LOCATION)) return (false, "")

    javaContainerPort = new RandomPortGenerator().getPort + ""

    val fileName = startCommand.split("\\s+")(1)

    val name = fileName.split("_")(1).replaceAll("\\.sh$", "")

    val startScript = execWithUserAndExitValue("", s"cat ${appDir}/$fileName", timeout)
      .out.split("\n").map { line =>
      if (line.startsWith("main=")) {
        line + " " + javaContainerPort
      } else if (line.startsWith("jvm=")) {
        ""
      } else line
    }.mkString("\n") + "\n" +
      s"jvm=-Xmx${DynamicDeploySlave.applicationMasterArguments.executeMemory}m -Xms${DynamicDeploySlave.applicationMasterArguments.executeMemory}m"

    Files.write(startScript, new java.io.File(s"${appDir}/$fileName"), Charset.forName("utf-8"))


    logger.info(s"appDir = $appDir fileName=$fileName name=$name newscript=$startScript")

    val runRes = execWithUserAndExitValue("", s"cd $appDir;$startCommand", timeout)

    val pid = execWithUserAndExitValue("", s"""  cd ${appDir}; cat pids/$name  """, timeout).out

    logger.info(s"containerId = $pid ")


    if (runRes.exitValue != 0) {
      logger.info(s"${imageUrl} run:${runRes.err}")
      sendMessage(imageUrl, "run:" + runRes.err)
      (false, "")
    } else {
      logger.info(s"${imageUrl} run:success:${pid}")
      sendMessage(imageUrl, s"run:success:${pid}")
      sendStartMessageToAM
      ShutdownHookManager.get().addShutdownHook(new Runnable {
        override def run(): Unit = {
          execWithUserAndExitValue("", s"cd ${appDir};./app.sh $fileName stop;rm -rf ${appDir}", 100 * 1000)
          exitMessageToAM
        }
      }, 1)
      javaContainerId.set(pid)
      (true, "")
    }
  }

  def exitMessageToAM = {
    sendMessage("-", "exit-" + InetAddress.getLocalHost.getHostAddress + ":" + javaContainerPort)
  }

  def sendStartMessageToAM = {
    sendMessage("-", "start-" + InetAddress.getLocalHost.getHostAddress + ":" + javaContainerPort)
  }

}
