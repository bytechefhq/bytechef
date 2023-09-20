
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.RemoteTriggerDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteTriggerDefinitionServiceClient extends AbstractWorkerClient
    implements RemoteTriggerDefinitionService {

    private static final String TRIGGER_DEFINITION_SERVICE = "/trigger-definition-service";

    public RemoteTriggerDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
                componentName, componentVersion, triggerName),
            TriggerDefinition.class);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-trigger-definitions/{componentName}/{componentVersion}",
                componentName,
                componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(List<ComponentOperation> componentOperations) {
        return CollectionUtils.map(
            componentOperations,
            componentOperation -> getTriggerDefinition(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName()));
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-webhook-trigger-flags/{componentName}/{componentVersion}" +
                    "/{triggerName}",
                componentName, componentVersion, triggerName),
            WebhookTriggerFlags.class);
    }
}
