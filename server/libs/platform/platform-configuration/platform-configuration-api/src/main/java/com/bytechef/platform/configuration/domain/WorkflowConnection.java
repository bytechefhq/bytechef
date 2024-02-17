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

/**
 * @param workflowNodeName action task/trigger name used in the workflow
 *
 * @author Ivica Cardic
 */
public record WorkflowConnection(
    String componentName, int componentVersion, String workflowNodeName, String key, boolean required) {

    public static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    public static final String COMPONENT_NAME = "componentName";
    public static final String COMPONENT_VERSION = "componentVersion";
    public static final String ID = "id";
    public static final String CONNECTIONS = "connections";
}
