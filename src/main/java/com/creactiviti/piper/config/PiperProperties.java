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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "piper")
public class PiperProperties {

  private SerializationProperties serialization;
  private PersistenceProperties persistence;
  private PipelineRepositoryProperties pipelineRepository;
  private MessageBrokerProperties messageBroker;
  private CoordinatorProperties coordinator = new CoordinatorProperties();
  private WorkerProperties worker = new WorkerProperties();

  public SerializationProperties getSerialization() {
    return serialization;
  }
  
  public void setSerialization(SerializationProperties aSerialization) {
    serialization = aSerialization;
  }
  
  public PersistenceProperties getPersistence() {
    return persistence;
  }
  
  public void setPersistence(PersistenceProperties aPersistence) {
    persistence = aPersistence;
  }
  
  public PipelineRepositoryProperties getPipelineRepository() {
    return pipelineRepository;
  }
  
  public void setPipelineRepository(PipelineRepositoryProperties aPipelineRepository) {
    pipelineRepository = aPipelineRepository;
  }

  public MessageBrokerProperties getMessageBroker() {
    return messageBroker;
  }
  
  public void setMessageBroker(MessageBrokerProperties aMessageBroker) {
    messageBroker = aMessageBroker;
  }

  public CoordinatorProperties getCoordinator() {
    return coordinator;
  }
  
  public void setCoordinator(CoordinatorProperties aCoordinator) {
    coordinator = aCoordinator;
  }
  
  public WorkerProperties getWorker() {
    return worker;
  }
  
  public void setWorker(WorkerProperties aWorker) {
    worker = aWorker;
  }
  
}
