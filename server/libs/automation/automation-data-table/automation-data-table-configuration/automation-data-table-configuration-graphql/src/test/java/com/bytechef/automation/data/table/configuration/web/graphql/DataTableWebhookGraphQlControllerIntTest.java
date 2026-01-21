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

package com.bytechef.automation.data.table.configuration.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.automation.data.table.configuration.web.graphql.config.DataTableGraphQlConfigurationSharedMocks;
import com.bytechef.automation.data.table.configuration.web.graphql.config.DataTableGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    DataTableGraphQlTestConfiguration.class,
    DataTableWebhookGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableWebhookGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@DataTableGraphQlConfigurationSharedMocks
public class DataTableWebhookGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    void testGetDataTableWebhooks() {
        // Given
        DataTableWebhookService.Webhook webhook1 = new DataTableWebhookService.Webhook(
            1L, 10L, "https://example.com/webhook1", DataTableWebhookType.RECORD_CREATED, 1L);
        DataTableWebhookService.Webhook webhook2 = new DataTableWebhookService.Webhook(
            2L, 10L, "https://example.com/webhook2", DataTableWebhookType.RECORD_UPDATED, 1L);

        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableWebhookService.listWebhooks("orders", 1L)).thenReturn(List.of(webhook1, webhook2));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableWebhooks(environmentId: "1", tableId: "10") {
                        id
                        url
                        type
                        environmentId
                    }
                }
                """)
            .execute()
            .path("dataTableWebhooks")
            .entityList(Object.class)
            .hasSize(2)
            .path("dataTableWebhooks[0].url")
            .entity(String.class)
            .isEqualTo("https://example.com/webhook1")
            .path("dataTableWebhooks[1].url")
            .entity(String.class)
            .isEqualTo("https://example.com/webhook2");
    }

    @Test
    void testGetDataTableWebhooksEmpty() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableWebhookService.listWebhooks("orders", 1L)).thenReturn(List.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableWebhooks(environmentId: "1", tableId: "10") {
                        id
                        url
                        type
                    }
                }
                """)
            .execute()
            .path("dataTableWebhooks")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testGetDataTableWebhooksWithDifferentTypes() {
        // Given
        DataTableWebhookService.Webhook webhook1 = new DataTableWebhookService.Webhook(
            1L, 10L, "https://example.com/create", DataTableWebhookType.RECORD_CREATED, 1L);
        DataTableWebhookService.Webhook webhook2 = new DataTableWebhookService.Webhook(
            2L, 10L, "https://example.com/update", DataTableWebhookType.RECORD_UPDATED, 1L);
        DataTableWebhookService.Webhook webhook3 = new DataTableWebhookService.Webhook(
            3L, 10L, "https://example.com/delete", DataTableWebhookType.RECORD_DELETED, 1L);

        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableWebhookService.listWebhooks("orders", 1L)).thenReturn(List.of(webhook1, webhook2, webhook3));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableWebhooks(environmentId: "1", tableId: "10") {
                        id
                        url
                        type
                    }
                }
                """)
            .execute()
            .path("dataTableWebhooks")
            .entityList(Object.class)
            .hasSize(3);
    }
}
