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

package com.bytechef.platform.data.table.execution.listener;

import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>
 * Selection is by the event's ref and its event type: the ref names the table the row was written into, and
 * {@link DataTableWebhookService#listWebhooks(DataTableRef)} answers for it. Selecting by base name instead would not
 * identify one registry row.
 *
 * @author Ivica Cardic
 */
@Component
public class DataTableWebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(DataTableWebhookEventListener.class);

    private final DataTableWebhookService dataTableWebhookService;
    private final RestTemplate restTemplate;

    @Autowired
    @SuppressFBWarnings("EI")
    public DataTableWebhookEventListener(DataTableWebhookService dataTableWebhookService) {
        this(dataTableWebhookService, new RestTemplate());
    }

    /**
     * Delivery is a data boundary, so a test has to be able to observe which URLs are actually posted to rather than
     * only which webhooks were selected.
     */
    @SuppressFBWarnings("EI")
    DataTableWebhookEventListener(DataTableWebhookService dataTableWebhookService, RestTemplate restTemplate) {
        this.dataTableWebhookService = dataTableWebhookService;
        this.restTemplate = restTemplate;
    }

    @EventListener
    @Async
    public void onDataTableWebhookEvent(DataTableWebhookEvent event) {
        DataTableRef dataTableRef = event.getDataTableRef();

        String baseName = dataTableRef.baseName();
        DataTableWebhookType type = event.getType();
        Map<String, Object> payload = event.getPayload();

        List<DataTableWebhookService.Webhook> hooks = dataTableWebhookService.listWebhooks(dataTableRef)
            .stream()
            .filter(webhook -> webhook.type() == type)
            .toList();

        if (hooks.isEmpty()) {
            return;
        }

        RetryTemplate retryTemplate = createRetryTemplate();

        for (DataTableWebhookService.Webhook hook : hooks) {

            Map<String, Object> body = new HashMap<>();

            body.put("type", type.name());
            body.put("table", baseName);
            body.put("timestamp", String.valueOf(Instant.now()));
            body.put("payload", payload);

            try {
                retryTemplate.execute(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Calling data table webhook {} -> {}", hook.url(), body);
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
