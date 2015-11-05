package org.scalameter
package utils

/**
 * Abstraction of the SystemUtils library to detect the OS
 */
trait AbstractSystemUtils {
  def IS_OS_WINDOWS : Boolean
}