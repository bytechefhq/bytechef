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

package com.bytechef.automation.data.table.execution.listener;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.automation.data.table.execution.event.DataTableWebhookEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.RestTemplate;

/**
 * Listener class for processing and handling {@link DataTableWebhookEvent} events asynchronously. This class responds
 * to webhook events associated with data table operations and invokes the appropriate webhooks for external system
 * notifications. <br>
 * The event listener fetches registered webhooks from the {@link DataTableWebhookService} for the given event's data
 * table and webhook type, and then executes HTTP POST requests to the retrieved webhook URLs with the event payload and
 * metadata.
 *
 * @author Ivica Cardic
 */
@Component
public class DataTableWebhookEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DataTableWebhookEventListener.class);

    private final DataTableWebhookService dataTableWebhookService;
    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressFBWarnings("EI")
    public DataTableWebhookEventListener(DataTableWebhookService dataTableWebhookService) {
        this.dataTableWebhookService = dataTableWebhookService;
    }

    @EventListener
    @Async
    public void onDataTableWebhookEvent(DataTableWebhookEvent event) {
        String baseName = event.getBaseName();
        long environmentId = event.getEnvironmentId();
        DataTableWebhookType type = event.getType();
        Map<String, Object> payload = event.getPayload();

        List<DataTableWebhookService.Webhook> hooks = dataTableWebhookService.listWebhooks(baseName, environmentId)
            .stream()
            .filter(webhook -> webhook.type() == type)
            .toList();

        if (hooks.isEmpty()) {
            return;
        }

        RetryTemplate retryTemplate = createRetryTemplate();

        for (DataTableWebhookService.Webhook hook : hooks) {

            Map<String, Object> body = new java.util.HashMap<>();

            body.put("type", type.name());
            body.put("table", baseName);
            body.put("timestamp", String.valueOf(Instant.now()));
            body.put("payload", payload);

            try {
                retryTemplate.execute(() -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Calling data table webhook {} -> {}", hook.url(), body);
                    }

                    return restTemplate.postForObject(hook.url(), body, String.class);
                });
            } catch (RetryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private RetryTemplate createRetryTemplate() {
        ExponentialBackOff backOff = new ExponentialBackOff(2000, 2.0);

        backOff.setMaxElapsedTime(60000);

        return new RetryTemplate(
            RetryPolicy.builder()
                .backOff(backOff)
                .build());
    }
}
