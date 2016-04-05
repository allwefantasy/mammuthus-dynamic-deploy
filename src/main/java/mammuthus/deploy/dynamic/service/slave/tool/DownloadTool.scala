package mammuthus.deploy.dynamic.service.slave.tool

import java.io.FileWriter

import scala.sys.process._

/**
 * 10/25/15 WilliamZhu(allwefantasy@gmail.com)
 * import scala.sys.process._
 */
object DownloadTool extends CommandExecTrait {
  def download(url: String, location: String, timeout: Long = 1000 * 60 * 5) = {
    execWithUserAndExitValue("", s""" wget -O "${location}" "${url}" """, timeout)
  }
}

case class ShellExecResult(exitValue: Int, err: String, out: String)

trait CommandExecTrait extends FileWriterTrait {

  def execWithUserAndExitValue(user: String, shellStr: String, timeout: Long) = {
    val out = new StringBuilder
    val err = new StringBuilder
    val et = ProcessLogger(
      line => out.append(line + "\n"),
      line => err.append(line + "\n"))

    val fileName = System.currentTimeMillis() + "_" + Math.random() + ".sh"
    writeToFile("/tmp/" + fileName, "#!/bin/bash\n" + shellStr)
    s"chmod u+x /tmp/$fileName".!
    val pb = Process(wrapCommand(user, fileName))
    val exitValue = pb ! et
    s"rm /tmp/$fileName".!
    ShellExecResult(exitValue, err.toString().trim, out.toString().trim)
  }

  def wrapCommand(user: String, fileName: String) = {
    if (user != null && !user.isEmpty) {
      s"su - $user /bin/bash -c '/bin/bash /tmp/$fileName'"
    } else s"/bin/bash /tmp/$fileName"
  }
}

trait FileWriterTrait {
  def using[A <: {def close() : Unit}, B](param: A)(f: A => B): B =
    try {
      f(param)
    } finally {
      param.close()
    }

  def writeToFile(fileName: String, data: String) =
    using(new FileWriter(fileName)) {
      fileWriter => fileWriter.append(data)
    }
}
