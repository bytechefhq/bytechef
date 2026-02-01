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

package com.bytechef.automation.knowledgebase.worker.config;

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentChunkEvent;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentEvent;
import com.bytechef.automation.knowledgebase.message.route.KnowledgeBaseMessageRoute;
import com.bytechef.automation.knowledgebase.worker.KnowledgeBaseDocumentProcessWorker;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.KnowledgeBase.Subscriptions;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPostReceiveProcessor;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseMessageBrokerConfigurerConfiguration {

    private final List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors;
    private final Subscriptions subscriptions;

    @SuppressFBWarnings("EI")
    KnowledgeBaseMessageBrokerConfigurerConfiguration(
        ApplicationProperties applicationProperties,
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors) {

        this.messageEventPostReceiveProcessors = messageEventPostReceiveProcessors;
        this.subscriptions = applicationProperties.getKnowledgeBase()
            .getSubscriptions();
    }

    @Bean
    MessageBrokerConfigurer<?> knowledgeBaseMessageBrokerConfigurer(
        KnowledgeBaseDocumentProcessWorker knowledgeBaseDocumentProcessWorker) {

        KnowledgeBaseDocumentProcessWorkerDelegate knowledgeBaseDocumentProcessWorkerDelegate =
            new KnowledgeBaseDocumentProcessWorkerDelegate(
                messageEventPostReceiveProcessors, knowledgeBaseDocumentProcessWorker);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> {
            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                KnowledgeBaseMessageRoute.DOCUMENT_PROCESS_EVENTS,
                subscriptions.getDocumentProcessEvents(), knowledgeBaseDocumentProcessWorkerDelegate,
                "onKnowledgeBaseDocumentEvent");

            messageBrokerListenerRegistrar.registerListenerEndpoint(
                listenerEndpointRegistrar,
                KnowledgeBaseMessageRoute.DOCUMENT_CHUNK_UPDATE_EVENTS,
                subscriptions.getDocumentChunkUpdateEvents(), knowledgeBaseDocumentProcessWorkerDelegate,
                "onKnowledgeBaseDocumentChunkEvent");
        };
    }

    private record KnowledgeBaseDocumentProcessWorkerDelegate(
        List<MessageEventPostReceiveProcessor> messageEventPostReceiveProcessors,
        KnowledgeBaseDocumentProcessWorker knowledgeBaseDocumentProcessWorker) {

        public void onKnowledgeBaseDocumentEvent(KnowledgeBaseDocumentEvent knowledgeBaseDocumentEvent) {
            TenantContext.runWithTenantId(
                (String) knowledgeBaseDocumentEvent.getMetadata(CURRENT_TENANT_ID),
                () -> knowledgeBaseDocumentProcessWorker.onKnowledgeBaseDocumentEvent(
                    (KnowledgeBaseDocumentEvent) process(knowledgeBaseDocumentEvent)));
        }

        public void onKnowledgeBaseDocumentChunkEvent(KnowledgeBaseDocumentChunkEvent knowledgeBaseDocumentChunkEvent) {
            TenantContext.runWithTenantId(
                (String) knowledgeBaseDocumentChunkEvent.getMetadata(CURRENT_TENANT_ID),
                () -> knowledgeBaseDocumentProcessWorker.onKnowledgeBaseDocumentChunkEvent(
                    (KnowledgeBaseDocumentChunkEvent) process(knowledgeBaseDocumentChunkEvent)));
        }

        private MessageEvent<?> process(MessageEvent<?> messageEvent) {
            for (MessageEventPostReceiveProcessor messageEventPostReceiveProcessor : messageEventPostReceiveProcessors) {
                messageEvent = messageEventPostReceiveProcessor.process(messageEvent);
            }

            return messageEvent;
        }
    }
}
