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

package com.bytechef.platform.webhook.executor.config;

import static com.bytechef.tenant.TenantContext.CURRENT_TENANT_ID;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
public class SseStreamMessageBrokerConfigurerConfiguration {

    @Bean
    MessageBrokerConfigurer<?> sseStreamMessageBrokerConfigurer(SseStreamBridgeRegistry sseStreamBridgeRegistry) {
        SseStreamDelegate sseStreamDelegate = new SseStreamDelegate(sseStreamBridgeRegistry);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
            .registerListenerEndpoint(
                listenerEndpointRegistrar, SseStreamMessageRoute.SSE_STREAM_EVENTS, 1, sseStreamDelegate,
                "onSseStreamEvent");
    }

    private record SseStreamDelegate(SseStreamBridgeRegistry sseStreamBridgeRegistry) {

        @SuppressFBWarnings("UPM")
        public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
            TenantContext.runWithTenantId(
                (String) sseStreamEvent.getMetadata(CURRENT_TENANT_ID),
                () -> sseStreamBridgeRegistry.onSseStreamEvent(sseStreamEvent));
        }
    }
}
