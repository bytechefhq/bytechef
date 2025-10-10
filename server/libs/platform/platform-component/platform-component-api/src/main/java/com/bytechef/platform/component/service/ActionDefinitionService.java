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
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionService {

    /**
     * Executes the routine for dynamic resolution of particular properties required for component action to properly
     * execute {@link #executeSingleConnectionPerform(String, int, String, Map, ComponentConnection, ActionContext)} or
     * {@link #executeMultipleConnectionsPerform(String, int, String, Map, Map, Map, ActionContext)} methods. Duration
     * is unpredictable as it may require connecting to outer APIs/microservices/platforms. Method is only called in
     * designTime, never in runtime. Every change of lookupDependsOnPaths parameter triggers this method to
     * automatically update the dependent propertyName.
     *
     * @param componentName        the name of component
     * @param componentVersion     the version
     * @param actionName           action name
     * @param propertyName         name of the property that requires dynamic resolution and is of
     *                             type @{@link com.bytechef.component.definition.Property.DynamicPropertiesProperty}
     * @param inputParameters      key-value collection of parameters required by business logic
     * @param lookupDependsOnPaths list of dependent property names
     * @param componentConnection  connection used to connect to outer source
     * @param context              additional technical data required by some actions
     * @return list of dynamically resolved properties
     * @throws {@link ConfigurationException} - if procession breaks within ByteChef system or
     *                {@link com.bytechef.component.exception.ProviderException} - if external system is unavailable or
     *                call to it results in errors
     */
    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection, ActionContext context);

    OutputResponse executeMultipleConnectionsOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> connections, Map<String, ?> extensions, ActionContext context);

    /**
     * Executes the action of particular component version which define perform function via
     * {@link com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction} interface. Duration is
     * unpredictable as work done by action may require connecting to outer APIs/microservices/platforms.
     *
     * @param componentName    the name of component
     * @param componentVersion the version
     * @param actionName       action name
     * @param inputParameters  key-value collection of parameters required by business logic
     * @param connections      collection of connections used to connect to outer sources
     * @param extensions
     * @param context          additional technical data required by some actions
     * @return result of execution or throws exceptions
     * @throws {@link ExecutionException} - if procession breaks within ByteChef system or
     *                {@link com.bytechef.component.exception.ProviderException} - if external system is unavailable or
     *                call to it results in errors
     */
    Object executeMultipleConnectionsPerform(
        String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters, Map<String, ComponentConnection> connections, Map<String, ?> extensions,
        ActionContext context);

    List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection, ActionContext context);

    ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode, Object body,
        ActionContext context);

    OutputResponse executeSingleConnectionOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection, ActionContext context);

    /**
     * Executes the action of particular component version which define perform function via
     * {@link com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction} interface. Duration is
     * unpredictable as work done by action may require connecting to outer APIs/microservices/platforms.
     *
     * @param componentName       the name of component
     * @param componentVersion    the version
     * @param actionName          action name
     * @param inputParameters     key-value collection of parameters required by business logic
     * @param componentConnection connection used to connect to outer sources
     * @param context             additional technical data required by some actions
     * @return result of execution or throws exceptions
     * @throws {@link ExecutionException} - if procession breaks within ByteChef system or
     *                {@link com.bytechef.component.exception.ProviderException} - if external system is unavailable or
     *                call to it results in errors
     */
    Object executeSingleConnectionPerform(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ActionContext context) throws ExecutionException;

    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ActionContext context);

    ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName);

    List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion);

    boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName);

    boolean isSingleConnectionPerform(String componentName, int componentVersion, String actionName);
}
