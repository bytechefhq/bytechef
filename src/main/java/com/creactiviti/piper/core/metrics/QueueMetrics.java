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
 */package com.creactiviti.piper.core.metrics;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.annotations.ConditionalOnCoordinator;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.QueueInfo;


/**
 * a {@link InfoContributor} implementation which exposes any declared AMQP
 * queues.
 * 
 * @author Arik Cohen
 * @since Apt 8, 2017
 */
@Component
@ConditionalOnCoordinator
@ConditionalOnProperty(name="piper.messenger.provider", havingValue="amqp")
public class QueueMetrics implements PublicMetrics {

  private Client client;
  
  @Override
  public Collection<Metric<?>> metrics() {
    List<QueueInfo> queues = client.getQueues();
    return queues.stream()
                 .map(this::queueInfoToMeticMapper)
                 .flatMap(List::stream)
                 .collect(Collectors.toList());
  }
  
  private List<Metric<Number>> queueInfoToMeticMapper (QueueInfo q) {
    return Arrays.asList(
      new Metric<>("queues."+q.getName()+".consumers",q.getConsumerCount()),
      new Metric<>("queues."+q.getName()+".messages.total",q.getTotalMessages()),
      new Metric<>("queues."+q.getName()+".messages.unacknowledged",q.getMessagesUnacknowledged()),
      new Metric<>("queues."+q.getName()+".messages.ready",q.getMessagesReady())
    );
  }

  @Autowired
  public void setClient(Client aClient) {
    client = aClient;
  }


}
