/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.core.messagebroker;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.error.Prioritizable;
import com.creactiviti.piper.core.error.Retryable;

public class AmqpMessageBroker implements MessageBroker {

  private AmqpTemplate amqpTemplate;

  @Override
  public void send (String aRoutingKey, Object aMessage) {
    Assert.notNull(aRoutingKey,"routing key can't be null");
    amqpTemplate.convertAndSend(determineExchange(aRoutingKey),determineRoutingKey(aRoutingKey),aMessage, (m) -> {
      if(aMessage instanceof Retryable) {
        Retryable r = (Retryable) aMessage;
        m.getMessageProperties().setDelay((int)r.getRetryDelayMillis());
      }
      if(aMessage instanceof Prioritizable) {
        Prioritizable p = (Prioritizable) aMessage;
        m.getMessageProperties().setPriority(p.getPriority());
      }
      return m;
    });
  }

  private String determineExchange (String aRoutingKey) {
    String[] routingKey = aRoutingKey.split("/");
    Assert.isTrue(routingKey.length<=2,"Invalid routing key: " + aRoutingKey);
    return routingKey.length==2?routingKey[0]:Exchanges.TASKS; 
  }
  
  private String determineRoutingKey (String aRoutingKey) {
    String[] routingKey = aRoutingKey.split("/");
    Assert.isTrue(routingKey.length<=2,"Invalid routing key: " + aRoutingKey);
    return routingKey.length==2?routingKey[1]:aRoutingKey; 
  }
  
  public void setAmqpTemplate(AmqpTemplate aAmqpTemplate) {
    amqpTemplate = aAmqpTemplate;
  }

}
