package java.io

class ObjectOutputStream(os : OutputStream) extends OutputStream {
  override def write(byte: Int): Unit = ???
  def writeObject(obj : Any) = ???
}