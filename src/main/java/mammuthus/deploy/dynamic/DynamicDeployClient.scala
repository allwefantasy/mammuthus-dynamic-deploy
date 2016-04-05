package mammuthus.deploy.dynamic


/**
 * 8/14/15 WilliamZhu(allwefantasy@gmail.com)
 */
class DynamicDeployClient {

}

object DynamicDeployClient {
  def main(args: Array[String]) = {

    (0 until 100).foreach { f =>
      println("i am master")
      Thread.sleep(1000)
    }
  }

}
