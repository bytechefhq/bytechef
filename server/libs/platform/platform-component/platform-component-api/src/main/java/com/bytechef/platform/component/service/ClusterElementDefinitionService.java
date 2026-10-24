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

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
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

    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections, boolean editorEnvironment);

    /**
     * Executes a tool inside a parent action's task execution. The tool's context is derived from the parent via
     * {@link ActionContextAware#toClusterElementContext} for every call, so the tool's log entries land under the
     * parent task in the execution view and a token-refresh retry runs against the refreshed connection. The derived
     * context carries the parent, so an {@code ActionContext} obtained from it via {@code toActionContext} forwards
     * {@code suspend(...)}, {@code getSuspend()}, the resume URL and the job resume id to the parent, and a suspending
     * tool (for example an AI agent's approval tool) suspends the parent's task.
     */
    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ActionContextAware actionContext);

    /**
     * Executes a tool with all of its connections inside a parent action's task execution, deriving the tool's context
     * from the parent the same way as
     * {@link #executeTool(String, int, String, Map, ComponentConnection, ActionContextAware)}.
     */
    Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        Map<String, ?> extensions, Map<String, ComponentConnection> componentConnections,
        ActionContextAware actionContext);

    /**
     * Executes an approval-channel cluster element (e.g. sending the approval request over Gmail, Slack, ...) on behalf
     * of a running approval action.
     */
    Object executeApprovalChannel(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        String formUrl, @Nullable ComponentConnection componentConnection, ActionContextAware actionContext);

    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters);

    /**
     * Returns {@code true} when the cluster element's owning component declares a connection definition. Used by the
     * shared component-interaction tooling (the in-editor Copilot) - e.g. the
     * {@code lookupClusterElementPropertyOptions} tool callback - to decide whether a property-options lookup must
     * carry a {@code connectionId}.
     *
     * @param componentName      the name of the component
     * @param componentVersion   the version of the component
     * @param clusterElementName the name of the cluster element (accepted for symmetry with sibling methods; the
     *                           connection requirement is currently a component-level concern)
     * @return {@code true} when the component declares a {@code ConnectionDefinition}, {@code false} otherwise
     */
    boolean clusterElementDefinesConnection(String componentName, int componentVersion, String clusterElementName);

    <T> T getClusterElement(String componentName, int componentVersion, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName);

    ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName, String clusterElementTypeName);

    List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType);

    List<ClusterElementDefinition> getClusterElementDefinitions(String componentName, int componentVersion);

    List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType);

    ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);

    /**
     * Returns the lookup-depends-on paths for a cluster element property's OptionsDataSource, or an empty list when the
     * property has no dynamic options OR cannot be resolved. {@code propertyName} accepts the same dotted paths as
     * {@code TriggerDefinitionService#getPropertyLookupDependsOn}.
     */
    List<String> getPropertyLookupDependsOn(
        String componentName, int componentVersion, String clusterElementName, String propertyName);

    List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName);

    /**
     * Returns true when the named cluster element property has a dynamic OptionsDataSource. The "not found" and "found
     * but no data source" cases are intentionally indistinguishable: both yield the safest fallback (the lookup gate
     * emits {@code no_options_for_property}).
     */
    boolean propertyHasOptionsDataSource(
        String componentName, int componentVersion, String clusterElementName, String propertyName);
}
