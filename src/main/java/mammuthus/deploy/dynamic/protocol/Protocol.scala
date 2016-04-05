package mammuthus.deploy.dynamic.protocol

/**
 * 10/28/15 WilliamZhu(allwefantasy@gmail.com)
 */
case class InstallPhase(image: String, status: String, time: Long) {
  override def toString() = {
    s"status=${status} image=${image} time=${time}"
  }
}
