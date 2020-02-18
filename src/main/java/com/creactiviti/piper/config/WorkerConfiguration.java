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
package com.creactiviti.piper.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.creactiviti.piper.core.Worker;
import com.creactiviti.piper.core.annotations.ConditionalOnWorker;
import com.creactiviti.piper.core.event.DistributedEventPublisher;
import com.creactiviti.piper.core.messagebroker.MessageBroker;
import com.creactiviti.piper.core.task.TaskHandlerResolver;

@Configuration
@ConditionalOnWorker
public class WorkerConfiguration {
  
  @Autowired @Lazy private MessageBroker messageBroker;
  
  @Bean
  Worker worker (TaskHandlerResolver aTaskHandlerResolver, MessageBroker aMessageBroker) {
    Worker worker = new Worker();
    worker.setMessageBroker(aMessageBroker);
    worker.setTaskHandlerResolver(aTaskHandlerResolver);
    worker.setEventPublisher(workerEventPublisher());
    return worker;
  }

  @Bean
  @Qualifier("Worker")
  DistributedEventPublisher workerEventPublisher () {
    return new DistributedEventPublisher (messageBroker);
  }
  
  
  
}
