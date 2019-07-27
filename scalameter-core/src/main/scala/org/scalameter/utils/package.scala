package org.scalameter



import javax.management._
import scala.jdk.CollectionConverters._


package object utils {

  def withGCNotification[T](eventhandler: Notification => Any)(body: => T) = {
    if (!currentContext(Key.preJDK7)) {
      val gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans
      val listeners = for (gcbean <- gcbeans.asScala) yield {
        val listener = new NotificationListener {
          def handleNotification(n: Notification, handback: Object): Unit = {
            eventhandler(n)
          }
        }
        val emitter = gcbean.asInstanceOf[NotificationEmitter]
        emitter.addNotificationListener(listener, null, null)
        (emitter, listener)
      }
      val result = body
      for ((emitter, l) <- listeners) emitter.removeNotificationListener(l)
      result
    } else {
      body
    }
  }

}