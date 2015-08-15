package org.scalameter
package persistence



import java.io._
import org.scalameter.Key.reports._



/** Serializes [[org.scalameter.History]] to binary format using Java serialization.
  */
case class SerializationPersistor(path: File)
  extends IOStreamPersistor[ObjectInputStream, ObjectOutputStream] {

  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExtension: String = "dat"

  protected def inputStream(file: File): ObjectInputStream =
    new ObjectInputStream(new FileInputStream(file)) {
    override def resolveClass(desc: ObjectStreamClass) = Class.forName(desc.getName)
  }

  protected def outputStream(file: File): ObjectOutputStream =
    new ObjectOutputStream(new FileOutputStream(file))

  protected def loadFrom[T](is: ObjectInputStream): History[T] =
    is.readObject().asInstanceOf[History[T]]

  protected def saveTo[T](history: History[T], os: ObjectOutputStream): Unit =
    os.writeObject(history)
}


object SerializationPersistor {

  def apply() = new SerializationPersistor

}

