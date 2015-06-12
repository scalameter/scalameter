package org.scalameter
package persistence



import java.io._
import Key.reports._



case class SerializationPersistor(path: File) extends IOStreamPersistor[ObjectInputStream, ObjectOutputStream] {

  def this(path: String) = this(new File(path))

  def this() = this(currentContext(resultDir))

  def fileExt: String = "dat"

  protected def inputStream(file: File): ObjectInputStream = new ObjectInputStream(new FileInputStream(file)) {
    override def resolveClass(desc: ObjectStreamClass) = Class.forName(desc.getName)
  }

  protected def outputStream(file: File): ObjectOutputStream= new ObjectOutputStream(new FileOutputStream(file))

  protected def loadFrom(is: ObjectInputStream): History = is.readObject().asInstanceOf[History]

  protected def saveTo(history: History, os: ObjectOutputStream): Unit = os.writeObject(history)
}


object SerializationPersistor {

  def apply() = new SerializationPersistor

}

