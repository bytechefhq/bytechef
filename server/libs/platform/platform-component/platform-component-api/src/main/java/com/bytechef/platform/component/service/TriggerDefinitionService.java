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

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionService extends OperationDefinitionService {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, List<String> lookupDependsOnPaths, @Nullable ComponentConnection componentConnection);

    WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName,
        @Nullable ComponentConnection componentConnection, Map<String, ?> outputParameters);

    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters);

    void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable ComponentConnection componentConnection);

    void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable ComponentConnection componentConnection);

    List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, @Nullable String searchText,
        @Nullable ComponentConnection componentConnection);

    @Nullable
    OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection);

    ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, @Nullable String componentOperationName,
        int statusCode, Object body, Map<String, List<String>> headers);

    TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, @Nullable Long jobPrincipalId,
        @Nullable String workflowUuid, Map<String, ?> inputParameters, @Nullable Object triggerState,
        @Nullable WebhookRequest webhookRequest, @Nullable ComponentConnection componentConnection,
        @Nullable Long environmentId, PlatformType type, boolean editorEnvironment);

    void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, @Nullable ComponentConnection componentConnection);

    @Nullable
    WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, String webhookUrl, @Nullable ComponentConnection componentConnection,
        long environmentId);

    WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, @Nullable ComponentConnection componentConnection);

    WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, @Nullable ComponentConnection componentConnection);

    /**
     * Returns the lookup-depends-on paths for a property's OptionsDataSource, or an empty list when the property has no
     * dynamic options OR cannot be resolved. Used by the shared component-interaction tooling (AI Hub and the in-editor
     * Copilot) to enforce dependency ordering before fetching options. {@code propertyName} accepts dotted paths:
     * {@code parent.child} descends into an ObjectProperty's children, {@code arrayProp[].child} is explicit descent
     * into an ArrayProperty's first item type, and {@code arrayProp.child} is implicit descent when the array has a
     * single object item type. Plain names match at the top level.
     */
    List<String> getPropertyLookupDependsOn(
        String componentName, int componentVersion, String triggerName, String propertyName);

    TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName);

    List<TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion);

    WebhookTriggerFlags getWebhookTriggerFlags(String componentName, int componentVersion, String triggerName);

    boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName);

    /**
     * Returns true when the named property has a dynamic OptionsDataSource. Used by the shared component-interaction
     * tooling (AI Hub and the in-editor Copilot) to short-circuit lookup calls that the LLM made for properties without
     * dynamic options. {@code propertyName} accepts dotted paths (see {@link #getPropertyLookupDependsOn} for the
     * supported conventions). The "not found" and "found but no data source" cases are intentionally indistinguishable:
     * both yield the safest fallback (the lookup gate emits {@code no_options_for_property}).
     */
    boolean propertyHasOptionsDataSource(
        String componentName, int componentVersion, String triggerName, String propertyName);

    /**
     * Returns {@code true} when the trigger's owning component declares a connection definition. Used by the shared
     * component-interaction tooling (AI Hub and the in-editor Copilot) - e.g. the {@code lookupTriggerPropertyOptions}
     * tool callback - to decide whether a property-options lookup must carry a {@code connectionId}. When the
     * underlying component has no connection, the lookup can proceed without one and the {@code connection_required}
     * precondition envelope is skipped.
     *
     * @param componentName    the name of the component
     * @param componentVersion the version of the component
     * @param triggerName      the name of the trigger (accepted for symmetry with sibling methods; the connection
     *                         requirement is currently a component-level concern)
     * @return {@code true} when the component declares a {@code ConnectionDefinition}, {@code false} otherwise
     */
    boolean triggerDefinesConnection(String componentName, int componentVersion, String triggerName);
}
