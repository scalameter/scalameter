package java.net

class InetAddress {
  def getHostName: String = "hostname"
}

object InetAddress {
  def getLocalHost: InetAddress = new InetAddress()
}