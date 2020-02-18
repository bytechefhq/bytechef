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

import java.util.concurrent.TimeUnit;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

import com.creactiviti.piper.core.error.Retryable;

public class JmsMessageBroker implements MessageBroker {

  private JmsTemplate jmsTemplate;

  @Override
  public void send (String aRoutingKey, Object aMessage) {
    Assert.notNull(aRoutingKey,"routing key can't be null");
    if(aMessage instanceof Retryable) {
      Retryable r = (Retryable) aMessage;
      delay(r.getRetryDelayMillis());
    }
    jmsTemplate.convertAndSend(aRoutingKey, aMessage);
  }
  
  private void delay (long aValue) {
    try {
      TimeUnit.MILLISECONDS.sleep(aValue);
    } catch (InterruptedException e) {
    }
  }

  public void setJmsTemplate(JmsTemplate aJmsTemplate) {
    jmsTemplate = aJmsTemplate;
  }

}
