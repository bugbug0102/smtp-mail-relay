package org.b0102.smtp.mail.relay

import org.subethamail.smtp.helper.SimpleMessageListener
import java.io.InputStream
import java.util.Properties

import javax.mail._
import javax.mail.internet.{InternetAddress, InternetHeaders, MimeMessage}
import org.slf4j.LoggerFactory
import javax.mail.Message.RecipientType

import collection.JavaConverters._

private[relay] class RelayMessageListener(private val relay:Relay) extends SimpleMessageListener
{
  private val logger = LoggerFactory.getLogger(classOf[RelayMessageListener])
  
  private val session:Session = 
  {
    val props = new Properties()
    props.put("mail.debug", if (relay.debug) "true" else "false")
    props.put("mail.smtp.host", relay.host)
    props.put("mail.smtp.port", relay.port.toString())
    props.put("mail.transport.protocol", relay.protocol)
    
    if(Option(relay.username).isDefined)
    {
  		props.put("mail.smtp.auth", "true")
  		props.put("mail.smtp.starttls.enable", "true")
      Session.getInstance(props, new Authenticator()
      {
        override def getPasswordAuthentication() = new PasswordAuthentication(relay.username, relay.password)
      })
    }else
    {
      Session.getInstance(props)  
    }
  }
  
  private def recipientsToString(arr:Array[Address]):String = 
  {
    if(Option(arr).isEmpty) return ""
    val sb = new StringBuffer(arr.toList.map(_.toString()).mkString(","))
    
    if(sb.length() > 0)
    {
       sb.substring(0, sb.length() - 2) 
    }
    return sb.toString()
    
  }
  
  override def accept(from:String, recipient:String):Boolean = true
  
  override def deliver(from:String, recipient:String, data:InputStream):Unit =
  {
    try
    {
      val mm = new MimeMessage(session, data)
      val newRecipient = relay.redirect
      val oldSubject = mm.getSubject
      val recipientRepresentation = s"${recipient} -> ${newRecipient}"
      
      val newSubject = s"[${relay.subjectPrefix}] ${oldSubject} ${recipientRepresentation}"
      mm.setSubject(newSubject, "UTF-8")
      
      if(logger.isDebugEnabled())
      {
        logger.debug(s"From\t:${recipientsToString(mm.getFrom)}")
        logger.debug(s"To\t:${recipientsToString(mm.getRecipients(RecipientType.TO))}")
        logger.debug(s"Cc\t:${recipientsToString(mm.getRecipients(RecipientType.CC))}")
        logger.debug(s"Old Subject\t:${oldSubject}")
        logger.debug(s"New Subject\t:${newSubject}")
        logger.debug(s"Recipient\t:${recipientRepresentation}")
        logger.debug("Redirect to\t:{}", newRecipient)
        mm.getAllHeaders.asScala.foreach{ h=>
          val he = h.asInstanceOf[Header]
          logger.debug("Header \t\t:{}->{}", Array(he.getName, he.getValue):_*)
        }
      }
      
      Transport.send(mm, Array(new InternetAddress(newRecipient)))
      logger.debug("Sent")
      
    }catch
    {
      case ex:MessagingException =>
      {
        logger.warn(ex.getMessage)
      }
    }
  }
  
}