package org.b0102.smtp.mail.relay

import java.io.IOException

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PropertiesLoaderUtils
import org.apache.commons.lang3.BooleanUtils
import org.subethamail.smtp.helper.SimpleMessageListener
import org.subethamail.smtp.server.SMTPServer
import org.springframework.beans.factory.annotation.Value
import collection.JavaConverters._
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter
import org.apache.commons.lang3.math.NumberUtils

@Configuration
@PropertySource(value= Array("classpath:smtp.mail.relay.properties"))
private[relay] class Config 
{
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
          val host = p.getProperty(s"org.b0102.smtp.mail.relay.${i}.host")
          val port_s = p.getProperty(s"org.b0102.smtp.mail.relay.${i}.port")
          val protocol = p.getProperty(s"org.b0102.smtp.mail.relay.${i}.protocol")
          val debug_s = p.getProperty(s"org.b0102.smtp.mail.relay.${i}.debug")
          val redirect = p.getProperty(s"org.b0102.smtp.mail.relay.${i}.redirect")
          val subjectPrefix = p.getProperty(s"org.b0102.smtp.mail.relay.${i}.subject.prefix")
          
          if(StringUtils.isNotBlank(host) && StringUtils.isNotBlank(port_s) && StringUtils.isNotBlank(protocol) && StringUtils.isNotBlank(debug_s) && StringUtils.isNotBlank(redirect) && StringUtils.isNotBlank(subjectPrefix))
          {
            val r = new Relay(host, NumberUtils.toInt(port_s, -1), protocol, BooleanUtils.toBoolean(debug_s), redirect, subjectPrefix)
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
    return ret.toList
  }
  
  @Bean
  private[relay] def simpleMessageListeners(relays:List[Relay]):List[SimpleMessageListener] = relays.map(r=>new RelayMessageListener(r)).toList
  
  
  @Bean
  private[relay] def smtpServer(@Value("${org.b0102.smtp.mail.relay.listen.host}") host:String, @Value("${org.b0102.smtp.mail.relay.listen.port}") port:Int, relays:List[SimpleMessageListener]):SMTPServer =
  {
    val ss = new SMTPServer(new SimpleMessageListenerAdapter(relays.asJava))
    ss.setHostName(host)
    ss.setPort(port)
    return ss 
  }
}