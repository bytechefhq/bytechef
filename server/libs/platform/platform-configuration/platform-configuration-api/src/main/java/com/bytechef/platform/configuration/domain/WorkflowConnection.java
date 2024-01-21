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
    private final String operationName; // task/trigger name used in the workflow
    private final boolean required;

    public WorkflowConnection(
        String componentName, int componentVersion, String operationName, String key, Long id, boolean required) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.id = id;
        this.key = key;
        this.operationName = operationName;
        this.required = required;
    }

    public String getComponentName() {
        return componentName;
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getKey() {
        return key;
    }

    public String getOperationName() {
        return operationName;
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
            ", operationName='" + operationName + '\'' +
            ", id=" + id +
            ", required=" + required +
            '}';
    }
}
