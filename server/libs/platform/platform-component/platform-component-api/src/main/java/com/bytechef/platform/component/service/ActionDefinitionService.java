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
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionService extends OperationDefinitionService {

    /**
     * Executes the routine for dynamic resolution of particular properties required for component action to properly is
     * unpredictable as it may require connecting to outer APIs/microservices/platforms. Method is only called in
     * designTime, never in runtime. Every change of the lookupDependsOnPaths parameter triggers this method to
     * automatically update the dependent propertyName.
     *
     * @param componentName        the name of the component
     * @param componentVersion     the version
     * @param actionName           action name
     * @param propertyName         name of the property that requires dynamic resolution and is of
     *                             type @{@link com.bytechef.component.definition.Property.DynamicPropertiesProperty}
     * @param inputParameters      key-value collection of parameters required by business logic
     * @param workflowId           id of the workflow that is currently executing
     * @param lookupDependsOnPaths list of dependent property names
     * @param componentConnection  connection used to connect to the outer source
     * @return list of dynamically resolved properties
     * @throws {@link com.bytechef.exception.ConfigurationException} - if procession breaks within ByteChef system or
     *                {@link com.bytechef.component.exception.ProviderException} - if an external system is unavailable
     *                or call to it results in errors
     */
    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId,
        @Nullable ComponentConnection componentConnection);

    /**
     * Executes the dynamic resolution of options for a specific property of a component. Used in design-time to update
     * selectable options dynamically based on input parameters or dependent property values. This method may establish
     * connections with external systems or APIs to fetch the required options.
     *
     * @param componentName        the name of the component
     * @param componentVersion     the version of the component
     * @param actionName           the name of the action triggering the option resolution
     * @param propertyName         the name of the property for which options are being resolved
     * @param inputParameters      a map containing input parameters required for business logic
     * @param lookupDependsOnPaths a list of dependent property paths that impact the options
     * @param searchText           the text query to filter the resolved options (if applicable)
     * @param componentConnection  an optional connection object for interactions with external systems
     * @return a list of {@link Option} objects dynamically resolved for the specified property
     * @throws com.bytechef.exception.ConfigurationException if there are issues within the system during execution
     * @throws ProviderException                             if the external systems or services are unavailable or
     *                                                       result in errors
     */
    List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection);

    /**
     * Executes the defined output logic for a specific component and its version, based on the given action name, input
     * parameters, and component connections. This method generates an output response that includes details such as the
     * output schema, sample output, or placeholder data.
     *
     * @param componentName        the name of the component
     * @param componentVersion     the version of the component
     * @param actionName           the name of the action to execute
     * @param inputParameters      a map of input parameters required for processing
     * @param componentConnections a map of component connections used for external interactions
     * @return the result of the output execution as an OutputResponse object
     */
    OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections);

    /**
     * Executes the perform logic for a specific component and its version, based on the given action name, mode type,
     * job details, workflow ID, input parameters, component connections, and optional extensions. This method processes
     * the request in the specified context and environment.
     *
     * @param componentName          the name of the component
     * @param componentVersion       the version of the component
     * @param actionName             the name of the action to execute
     * @param jobPrincipalId         the ID of the job principal
     * @param jobPrincipalWorkflowId the workflow ID associated with the job principal
     * @param jobId                  the job ID
     * @param workflowId             the ID of the workflow being processed
     * @param inputParameters        a map containing input parameters required for processing
     * @param componentConnections   a map of component connections used for external interactions
     * @param extensions             a map of optional extensions to further control the execution
     * @param editorEnvironment      a flag indicating whether the execution is performed in an editor environment
     * @param type                   the mode type (e.g., AUTOMATION or EMBEDDED)
     * @return an object representing the result of the performed action
     */
    Object executePerform(
        String componentName, int componentVersion, String actionName, Long jobPrincipalId, Long jobPrincipalWorkflowId,
        Long jobId, String workflowId, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions, boolean editorEnvironment,
        ModeType type);

    /**
     * Executes the perform logic for a specific component and its version in the context of polyglot execution. This
     * method processes the given action within a specific execution context and establishes a connection to an external
     * component if needed.
     *
     * @param componentName       the name of the component to execute
     * @param componentVersion    the version of the component
     * @param actionName          the name of the action to be performed
     * @param inputParameters     a map of input parameters required for processing
     * @param componentConnection an instance of {@link ComponentConnection} used for external interactions
     * @param actionContext       the context in which the action is executed
     * @return an object representing the result of the performed action
     */
    Object executePerformForPolyglot(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection, ActionContext actionContext);

    /**
     * Processes the error response for a given component and action and returns a ProviderException.
     *
     * @param componentName    the name of the component where the error occurred
     * @param componentVersion the version of the component where the error occurred
     * @param actionName       the name of the action being executed when the error occurred
     * @param statusCode       the HTTP status code associated with the error response
     * @param body             the body of the error response containing additional error details
     * @return a ProviderException detailing the processed error response
     */
    ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode, Object body);

    /**
     * Executes a workflow node description based on the specified component, version, action, and input parameters.
     * This method processes the workflow node and returns a description of the execution result.
     *
     * @param componentName    the name of the component to be executed
     * @param componentVersion the version of the component to be executed
     * @param actionName       the action name to be executed within the component
     * @param inputParameters  a map of input parameters required for executing the workflow node
     * @return a string representing the description of the execution result
     */
    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters);

    /**
     * Retrieves the action definition for a specified component and action.
     *
     * @param componentName    the name of the component for which the action definition is to be retrieved
     * @param componentVersion the version of the component for which the action definition is to be retrieved
     * @param actionName       the name of the action whose definition is to be retrieved
     * @return the action definition corresponding to the provided component name, version, and action name
     */
    ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName);

    /**
     * Retrieves a list of action definitions for a specified component name and version.
     *
     * @param componentName    the name of the component for which the action definitions are being retrieved
     * @param componentVersion the version of the component for which the action definitions are being retrieved
     * @return a list of ActionDefinition objects associated with the specified component name and version
     */
    List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion);

    /**
     * Checks if a dynamic output is defined for the given component, version, and action.
     *
     * @param componentName    the name of the component to check
     * @param componentVersion the version of the component to check
     * @param actionName       the name of the action to check within the component
     * @return true if a dynamic output is defined, false otherwise
     */
    boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName);
}
