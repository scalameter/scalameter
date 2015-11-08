package org.scalameter


/** TODO : Decide wheather it would make sense 
 *  (is possible) to have GC notifications on a
 *  js virtual machine
 */

package object utils {
  type Notification = Any

  def withGCNotification[T](eventhandler: Notification => Any)(body: => T) = {
    body
  }

}