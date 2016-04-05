package mammuthus.deploy.dynamic

import mammuthus.deploy.dynamic.service.slave.HeartBeatService
import mammuthus.yarn.{ExecutorBackend, ExecutorBackendParam}
import net.csdn.ServiceFramwork
import net.csdn.bootstrap.Application

/**
 * 10/24/15 WilliamZhu(allwefantasy@gmail.com)
 */

object DynamicDeploySlave extends ExecutorBackend {
  var applicationMasterArguments: ExecutorBackendParam = null

  def main(args: Array[String]) = {
    applicationMasterArguments = parse(args)
    ServiceFramwork.applicaionYamlName("application.slave.yml")
    ServiceFramwork.scanService.setLoader(classOf[DynamicDeploySlave])
    ServiceFramwork.disableDubbo()
    ServiceFramwork.registerStartWithSystemServices(classOf[HeartBeatService])
    Application.main(args)
  }
}

class DynamicDeploySlave
