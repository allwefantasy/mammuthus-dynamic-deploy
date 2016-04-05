package mammuthus.deploy.dynamic.service.slave.tool

/**
 * 10/25/15 WilliamZhu(allwefantasy@gmail.com)
 */
object LoadImageTool extends CommandExecTrait {


  def loadImage(location: String, timeout: Long) = {
    val fileName = location.split("/").last
    val Array(imageName, tag) = fileName.split("@")
    val res = execWithUserAndExitValue("", s"""docker images ${imageName}|grep ${tag.replaceAll(".image", "")}""", timeout)
    if (res.exitValue != 0) {
      val command = s"docker load <  ${location}"
      println(command)
      execWithUserAndExitValue("", command, timeout)
    } else {
      res
    }
  }

  def loadTarGz(applicationInstallDir:String,location: String, timeout: Long) = {
    val tarFileName = location.split("/").last
    val time = System.currentTimeMillis()
    val appDir = s"${applicationInstallDir}/${time}"
    val command = s"" +
      s"mkdir -p $appDir;" +
      s"cp -f ${location} ${appDir}/;" +
      s"cd ${appDir};" +
      s"tar xzf ${tarFileName}"
    (appDir, execWithUserAndExitValue("", command, timeout))
  }
}
