/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Arik Cohen
 */
@ConfigurationProperties(prefix = "atlas")
public class AtlasProperties {

    private CoordinatorProperties coordinator = new CoordinatorProperties();
    private MessageBrokerProperties messageBroker;
    private PersistenceProperties persistence;
    private SerializationProperties serialization;
    private StorageProperties storage;
    private WorkflowRepositoryProperties workflowRepository;
    private WorkerProperties worker = new WorkerProperties();

    public CoordinatorProperties getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(CoordinatorProperties coordinator) {
        this.coordinator = coordinator;
    }

    public MessageBrokerProperties getMessageBroker() {
        return messageBroker;
    }

    public void setMessageBroker(MessageBrokerProperties messageBroker) {
        this.messageBroker = messageBroker;
    }

    public PersistenceProperties getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceProperties persistence) {
        this.persistence = persistence;
    }

    public SerializationProperties getSerialization() {
        return serialization;
    }

    public void setSerialization(SerializationProperties serialization) {
        this.serialization = serialization;
    }

    public StorageProperties getStorage() {
        return storage;
    }

    public void setStorage(StorageProperties storage) {
        this.storage = storage;
    }

    public WorkflowRepositoryProperties getWorkflowRepository() {
        return workflowRepository;
    }

    public void setWorkflowRepository(WorkflowRepositoryProperties workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    public WorkerProperties getWorker() {
        return worker;
    }

    public void setWorker(WorkerProperties worker) {
        this.worker = worker;
    }
}
