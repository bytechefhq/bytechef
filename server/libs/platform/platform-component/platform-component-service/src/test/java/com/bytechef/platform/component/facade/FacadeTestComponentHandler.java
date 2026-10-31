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

package com.bytechef.platform.component.facade;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A component registered only in the facade IntTests. Its functions run locally and answer with the connection
 * parameters they receive, so a test can prove that a facade loaded the connection row from the database and handed
 * it to the real definition service.
 *
 * <p>
 * Deliberately not a Spring stereotype: it is registered through {@code @SpringBootTest(classes = ...)} so the
 * component scan of {@code ComponentRegistryConfiguration} never picks it up for other tests.
 *
 * @author Ivica Cardic
 */
public class FacadeTestComponentHandler implements ComponentHandler {

    public static final String COMPONENT_NAME = "facadeTest";
    public static final String CHANNEL_ID = "channelId";
    public static final String NO_CONNECTION = "none";

    public static final ClusterElementType PROBE = new ClusterElementType("PROBE", "probe", "Probe");
    public static final ClusterElementType SOURCE = new ClusterElementType("SOURCE", "source", "Source");

    private final List<String> invocations = new CopyOnWriteArrayList<>();

    private final ComponentDefinition componentDefinition = component(COMPONENT_NAME)
        .title("Facade Test")
        .connection(
            connection()
                .authorizations(
                    authorization(AuthorizationType.API_KEY)
                        .properties(string(CHANNEL_ID))))
        .inputs(
            string("channel")
                .options((OptionsFunction<String>) this::getChannelOptions))
        .clusterElements(
            clusterElement("probe")
                .title("Probe")
                .type(PROBE)
                .object(() -> "probe")
                .properties(
                    dynamicProperties("schemas")
                        .properties((ClusterElementDefinition.PropertiesFunction) this::getProbeProperties)),
            clusterElement("source")
                .title("Source")
                .type(SOURCE)
                .object(() -> "source"))
        .triggers(
            trigger("newEvent")
                .type(TriggerType.DYNAMIC_WEBHOOK)
                .webhookEnable(
                    (inputParameters, connectionParameters, webhookUrl, workflowExecutionId, context) -> {
                        recordInvocation("webhookEnable", connectionParameters);

                        return new WebhookEnableOutput(
                            Map.of("subscription", getChannelId(connectionParameters) + "_" + webhookUrl), null);
                    })
                .webhookDisable(
                    (inputParameters, connectionParameters, outputParameters, workflowExecutionId, context) -> recordInvocation(
                        "webhookDisable:" + outputParameters.getString("subscription"), connectionParameters))
                .dynamicWebhookRefresh(
                    (connectionParameters, outputParameters, context) -> new WebhookEnableOutput(
                        Map.of(
                            "subscription",
                            getChannelId(connectionParameters) + "_refreshed_" +
                                outputParameters.getString("subscription")),
                        null))
                .webhookRequest(
                    (inputParameters, connectionParameters, headers, parameters, body, method, output, context) -> null),
            trigger("listen")
                .type(TriggerType.LISTENER)
                .listenerEnable(
                    (inputParameters, connectionParameters, workflowExecutionId, listenerEmitter, context) -> recordInvocation(
                        "listenerEnable:" + workflowExecutionId, connectionParameters))
                .listenerDisable(
                    (inputParameters, connectionParameters, workflowExecutionId, context) -> recordInvocation(
                        "listenerDisable:" + workflowExecutionId, connectionParameters)));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    public void clearInvocations() {
        invocations.clear();
    }

    public List<String> getInvocations() {
        return List.copyOf(invocations);
    }

    private static String getChannelId(Parameters connectionParameters) {
        return connectionParameters.getString(CHANNEL_ID, NO_CONNECTION);
    }

    private List<com.bytechef.component.definition.Option<String>> getChannelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, com.bytechef.component.definition.Context context) {

        if (!connectionParameters.containsKey(CHANNEL_ID)) {
            return List.of();
        }

        return List.of(option("Channel", getChannelId(connectionParameters)));
    }

    private List<? extends ValueProperty<?>> getProbeProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ClusterElementContext context) {

        String resolvedSource = "unresolved";

        if (inputParameters.getBoolean("resolve", false)) {
            ClusterElementContextAware clusterElementContextAware = (ClusterElementContextAware) context;

            String resolvedValue = clusterElementContextAware.resolveClusterElement(
                SOURCE,
                (clusterElement, sourceInputParameters, sourceConnectionParameters, extensions, componentConnections,
                    sourceContext) -> sourceInputParameters.getString("label", "noLabel") + "_" +
                        getChannelId(sourceConnectionParameters));

            if (resolvedValue != null) {
                resolvedSource = resolvedValue;
            }
        }

        return List.of(string(getChannelId(connectionParameters) + "_" + resolvedSource));
    }

    private void recordInvocation(String operation, Parameters connectionParameters) {
        invocations.add(operation + "@" + getChannelId(connectionParameters));
    }
}
