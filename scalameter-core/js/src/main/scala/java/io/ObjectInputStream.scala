package java.io

class ObjectInputStream(is : InputStream) extends InputStream {
  def read: Int = ???
  def readObject() : Any = ???
}