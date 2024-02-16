/*
 * Copyright 2023-present ByteChef Inc.
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
 */

package com.bytechef.platform.configuration.domain;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class WorkflowConnection {

    public static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    public static final String COMPONENT_NAME = "componentName";
    public static final String COMPONENT_VERSION = "componentVersion";
    public static final String ID = "id";
    public static final String CONNECTIONS = "connections";

    private final String componentName;
    private final int componentVersion;
    private final Long id;
    private final String key;
    private final boolean required;
    private final String workflowNodeName; // action task/trigger name used in the workflow

    public WorkflowConnection(
        String componentName, int componentVersion, String workflowNodeName, String key, Long id, boolean required) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.id = id;
        this.key = key;
        this.required = required;
        this.workflowNodeName = workflowNodeName;
    }

    public String getComponentName() {
        return componentName;
    }

    public Optional<Long> fetchId() {
        return Optional.ofNullable(id);
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getKey() {
        return key;
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "WorkflowConnection{" +
            "componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", key='" + key + '\'' +
            ", workflowNodeName='" + workflowNodeName + '\'' +
            ", id=" + id +
            ", required=" + required +
            '}';
    }
}
