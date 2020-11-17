package org.b0102.smtp.mail.relay

import java.io.IOException
import java.util.Properties

import org.apache.commons.lang3.{BooleanUtils, StringUtils}
import org.apache.commons.lang3.math.NumberUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.{Bean, Configuration, PropertySource}
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import org.subethamail.smtp.helper.{SimpleMessageListener, SimpleMessageListenerAdapter}
import org.subethamail.smtp.server.SMTPServer

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.{break, breakable}

@PropertySource(value= Array("classpath:smtp.mail.relay.properties"))
@Configuration
private[relay] class Config
{
  private def getProperty(properties:Properties, key:String):String =
  {
    val p = System.getenv(key)
    if(StringUtils.isBlank(p)) properties.getProperty(key) else p
  }

  @Bean
  private[relay] def relays():List[Relay] = 
  {
    val ret = new ListBuffer[Relay]()
    val res = new ClassPathResource("/smtp.mail.relay.properties")
    try
    {
      breakable
      {
        val p = PropertiesLoaderUtils.loadProperties(res)
        var i = 0
        while(true)
        {

          val host = getProperty(p, s"org.b0102.smtp.mail.relay.${i}.host")
          val port_s = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.port")
          val protocol = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.protocol")
          val username = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.username")
          val password = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.password")
          val debug_s = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.debug")
          val redirect = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.redirect")
          val subjectPrefix = getProperty(p,s"org.b0102.smtp.mail.relay.${i}.subject.prefix")
          
          if(StringUtils.isNotBlank(host) && StringUtils.isNotBlank(port_s) && StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(debug_s) && StringUtils.isNotBlank(redirect) && StringUtils.isNotBlank(subjectPrefix))
          {
            val r = new Relay(host, NumberUtils.toInt(port_s, -1), protocol, username, password, BooleanUtils.toBoolean(debug_s), redirect, subjectPrefix)
            ret += r
            i += 1
            
          }else
          {
            break
          }
        }
      }
      
    }catch
    {
      case ex:IOException => throw new RuntimeException(ex) 
    }
    ret.toList
  }
  
  @Bean
  private[relay] def simpleMessageListeners(relays:List[Relay]):List[SimpleMessageListener] = relays.map(r=>new RelayMessageListener(r)).toList
  
  
  @Bean
  private[relay] def smtpServer(@Value("${org.b0102.smtp.mail.relay.listen.host}") host:String, @Value("${org.b0102.smtp.mail.relay.listen.port}") port:Int, relays:List[SimpleMessageListener]):SMTPServer =
  {
    val ss = new SMTPServer(new SimpleMessageListenerAdapter(relays.asJava))
    ss.setHostName(host)
    ss.setPort(port)
    ss
  }
}