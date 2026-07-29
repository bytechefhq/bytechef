/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.service;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Field;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementDefinitionService extends OperationDefinitionService {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection);

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection,
        ClusterElementResolverFunction clusterElementResolver);

    List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection, ClusterElementResolverFunction clusterElementResolver);

    /**
     * Workflow-less variant of {@link com.bytechef.component.definition.datastream.FieldsProvider#getFields} surfaced
     * for the Add Context Source wizard. Returns an empty list when the cluster element does not implement
     * {@code FieldsProvider} (the wizard then falls back to a free-text input).
     */
    List<Field> executeFields(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection);

    @Nullable
    OutputResponse executeOutput(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection);

    Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment);

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment);

    /**
     * Executes a single-connection tool cluster element, threading the supplied AI agent {@link ActionContext} through
     * so the tool runs against the agent's context (enabling {@code suspend()}/{@code resume()} to reach the live agent
     * execution). When {@code agentActionContext} is {@code null} this behaves exactly like the overload without it.
     */
    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment,
        @Nullable ActionContext agentActionContext);

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment);

    /**
     * Executes an approval-channel cluster element (e.g. sending the approval request over Gmail, Slack, ...) on behalf
     * of a running approval action.
     */
    Object executeApprovalChannel(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        String formUrl, @Nullable ComponentConnection componentConnection, ActionContextAware actionContext);

    /**
     * Executes a multi-connection tool cluster element, threading the supplied AI agent {@link ActionContext} through
     * so the tool runs against the agent's context (enabling {@code suspend()}/{@code resume()} to reach the live agent
     * execution). When {@code agentActionContext} is {@code null} this behaves exactly like the overload without it.
     */
    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment,
        @Nullable ActionContext agentActionContext);

    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters);

    <T> T getClusterElement(String componentName, int componentVersion, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName, String clusterElementTypeName);

    List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType);

    /**
     * List-view variant of {@link #getClusterElementDefinitions(ClusterElementType)} that returns lightweight stubs
     * (identity, texts, type — no property tree) sourced from the build-time component index without loading any
     * component handler. Use for enumeration/population; use the full method when property trees are required.
     */
    List<ClusterElementDefinition> getClusterElementDefinitionStubs(ClusterElementType clusterElementType);

    List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType);

    ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);

    List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);
}
