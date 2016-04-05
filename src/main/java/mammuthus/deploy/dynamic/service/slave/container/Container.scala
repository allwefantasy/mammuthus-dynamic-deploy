package mammuthus.deploy.dynamic.service.slave.container

import mammuthus.deploy.dynamic.service.slave.MasterClient
import mammuthus.deploy.dynamic.service.slave.tool.{CommandExecTrait, DownloadTool}

/**
 * all application install should have these three steps,_download,_load,and _run.
 *
 */
trait Container extends CommandExecTrait {

  def APP_LOCATION:String

  protected def _download(imageUrl: String, location: String, startCommand: String): (Boolean, String) = {
    val items = location.split("/")
    val dir = items.take(items.size - 1).mkString("/") + "/" + System.currentTimeMillis()
    execWithUserAndExitValue("", s"mkdir -p $dir", 1000)
    val newLocation = dir + "/" + items.last
    val downloadRes = DownloadTool.download(imageUrl, newLocation, timeout)
    if (downloadRes.exitValue != 0) {
      sendMessage(imageUrl, "download:" + downloadRes.err)
      (false, newLocation)
    }
    else {
      sendMessage(imageUrl, "download:success")
      (true, newLocation)
    }
  }


  def runContainer(imageUrl: String, location: String, startCommand: String, phase: String) = {

    new Thread(new Runnable {
      override def run(): Unit = {

        phase match {
          case "download" => _download(imageUrl, location, startCommand)
          case "load" => _load(APP_LOCATION,imageUrl, location, startCommand)
          case "run" => _run(imageUrl, location, startCommand)
          case _ =>
            var shouldNext = _download(imageUrl, location, startCommand)
            val newLocation = shouldNext._2
            if (shouldNext._1) shouldNext = _load(APP_LOCATION,imageUrl, newLocation, startCommand)
            if (shouldNext._1) shouldNext = _run(imageUrl, shouldNext._2, startCommand)
        }


      }
    }).start()
  }

  protected def _load(appLocation:String,imageUrl: String, location: String, startCommand: String): (Boolean, String)

  protected def _run(imageUrl: String, location: String, startCommand: String): (Boolean, String)


  protected def sendMessage(imageUrl: String, msg: String) = {
    MasterClient.masterClient.containerInstallPhase(containerHostAndPort, imageUrl, msg)
  }

  protected def containerHostAndPort: String

  protected def timeout: Long

}
