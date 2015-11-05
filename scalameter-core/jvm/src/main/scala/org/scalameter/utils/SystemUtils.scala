package org.scalameter
package utils

/**
 *  SystemUtils for the JVM
 */
object SystemUtils extends AbstractSystemUtils {
  def IS_OS_WINDOWS : Boolean = {
    org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS
  }
}