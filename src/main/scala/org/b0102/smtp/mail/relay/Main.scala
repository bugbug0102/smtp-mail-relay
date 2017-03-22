package org.b0102.smtp.mail.relay

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.subethamail.smtp.server.SMTPServer

object Main 
{
  private val logger = LoggerFactory.getLogger(this.getClass)
  def main(args:Array[String]):Unit = 
  {
    val ctx = new AnnotationConfigApplicationContext(classOf[Config])
    val ss = ctx.getBean(classOf[SMTPServer])
    
    logger.debug("SMTP Mail Relay Started")
    ss.start()
    
    Runtime.getRuntime.addShutdownHook(new Thread()
    {
      override def run():Unit =
      {
        ss.stop()
        ctx.close()
        logger.debug("SMTP Mail Relay Closed")
      }
    })
      
    
  }
  
}