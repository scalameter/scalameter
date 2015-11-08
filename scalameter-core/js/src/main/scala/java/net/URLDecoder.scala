package java.net

import java.nio.CharBuffer

object URLDecoder {
  def decode(str: String, encoding: String): String = {
    val withSpaces = str.replace('+', ' ')
    val charBuffer = CharBuffer.allocate(withSpaces.length)

    var i = 0
    while (i < withSpaces.length) {
      if (withSpaces(i) == '%' && i + 3 < withSpaces.length) {
        try {
          val hexValue = withSpaces.substring(i + 1, i + 3)
          val byteValue = Integer.parseInt(hexValue, 16).toByte
          charBuffer.append(new String(Array(byteValue), encoding))
          i += 3
        } catch {
          case _: NumberFormatException =>
            charBuffer.append('%')
            i += 1
        }
      } else {
        charBuffer.append(withSpaces(i))
        i += 1
      }
    }

    charBuffer.toString()
  }
}