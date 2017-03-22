package org.b0102.smtp.mail.relay

@SerialVersionUID(1L)
private[relay] class Relay 
  (val host:String, val port:Int, val protocol:String, val debug:Boolean, val redirect:String, val subjectPrefix:String)
  extends Serializable with Equals
{
  def this() = this(null, -1, null, false, null, null) 
  
  override def canEqual(other:Any) = other.isInstanceOf[Relay]
  
  override def hashCode = 41 * (41 + Option(redirect).getOrElse("").toInt + port.hashCode())
  
  override def equals(other:Any) = other match
  {
    case that: Relay =>  Option(this.redirect).getOrElse("") == Option(that.redirect).getOrElse("") && this.port == that.port 
    
    case _ => false
  }
}