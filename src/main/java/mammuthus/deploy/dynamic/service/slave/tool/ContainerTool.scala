package mammuthus.deploy.dynamic.service.slave.tool

/**
 * 10/25/15 WilliamZhu(allwefantasy@gmail.com)
 */
object ContainerTool extends CommandExecTrait {
  def runContainer(command: String, timeout: Long) = {
    execWithUserAndExitValue("", command, timeout)
  }
}
