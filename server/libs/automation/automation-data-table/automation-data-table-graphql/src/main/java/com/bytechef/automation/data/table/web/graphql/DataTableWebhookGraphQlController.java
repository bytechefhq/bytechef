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

package com.bytechef.automation.data.table.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class DataTableWebhookGraphQlController {

    private final DataTableService dataTableService;
    private final DataTableWebhookService dataTableWebhookService;
    private final EnvironmentService environmentService;

    public DataTableWebhookGraphQlController(
        DataTableService dataTableService, DataTableWebhookService dataTableWebhookService,
        EnvironmentService environmentService) {

        this.dataTableService = dataTableService;
        this.dataTableWebhookService = dataTableWebhookService;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<Webhook> dataTableWebhooks(@Argument Long environmentId, @Argument Long tableId) {
        environmentService.getEnvironment(environmentId);

        String baseName = dataTableService.getBaseNameById(tableId);

        return dataTableWebhookService.listWebhooks(baseName, environmentId)
            .stream()
            .map(webhook -> new Webhook(webhook.id(), webhook.url(), webhook.type(), webhook.environmentId()))
            .toList();
    }

    public record Webhook(Long id, String url, DataTableWebhookType type, long environmentId) {
    }
}
