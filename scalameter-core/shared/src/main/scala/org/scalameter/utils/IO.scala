package org.scalameter.utils

import java.io.{File, FileInputStream, ByteArrayOutputStream, InputStream}
import scala.annotation.tailrec


object IO {
  /** Reads all bytes from given [[java.io.InputStream]].
   *
   *  Note that this method does not close supplied stream.
   */
  def readFromInputStream(from: InputStream, chunkSize: Int = 2048): Array[Byte] = {
    val buffer = new Array[Byte](chunkSize)
    val output = new ByteArrayOutputStream(chunkSize)

    @tailrec
    def readBytes() {
      val bytes = from.read(buffer)
      if (bytes > -1) {
        output.write(buffer, 0, bytes)
        readBytes()
      }
    }

    readBytes()
    output.toByteArray
  }

  /** Reads all bytes from given [[java.io.File]]. */
  def readFromFile(from: File, chunkSize: Int = 2048): Array[Byte] = {
    val fis = new FileInputStream(from)
    val bytes = readFromInputStream(fis, chunkSize)
    fis.close()
    bytes
  }
}
