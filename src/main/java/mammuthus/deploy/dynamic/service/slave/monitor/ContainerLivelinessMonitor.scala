package mammuthus.deploy.dynamic.service.slave.monitor

import java.util.concurrent.atomic.{AtomicReference, AtomicInteger}
import java.util.concurrent.{Executors, TimeUnit}

import com.google.inject.Singleton
import net.csdn.common.logging.Loggers

/**
 * 10/30/15 WilliamZhu(allwefantasy@gmail.com)
 */
@Singleton
class ContainerLivelinessMonitor {
  val logger = Loggers.getLogger(getClass)

  val containerLivelinessMonitor = Executors.newSingleThreadScheduledExecutor()
  val checkLivelinessAttemptCounter = new AtomicInteger(0)
  val MaxCheckLivelinessAttemptTimes = 1

  def monitor(containerId: AtomicReference[String], checkLive: (String) => Boolean, shutdown: (String) => Unit) = {
    containerLivelinessMonitor.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        if (containerId.get() != null) {
          val live = checkLive(containerId.get())
          if (!live) {
            checkLivelinessAttemptCounter.incrementAndGet()
            if (checkLivelinessAttemptCounter.get() >= MaxCheckLivelinessAttemptTimes) {
              logger.error(s"Container $containerId may crack unexpected. and check with ${MaxCheckLivelinessAttemptTimes} times. Exist")
              shutdown(containerId.get())
            }
          }
        }
      }
    }, 10, 1, TimeUnit.SECONDS)
  }
}
