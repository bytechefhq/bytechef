/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.monday.trigger;

import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.TEXT;
import static com.bytechef.component.monday.constant.MondayConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.monday.util.MondayUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MondayNewItemInBoardTriggerTest {

    private final WebhookEnableOutput mockedDynamicWebhookEnableOutput = mock(WebhookEnableOutput.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Parameters parameters = MockParametersFactory.create(Map.of(BOARD_ID, "board"));

    @Test
    void testWebhookEnable() {
        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(anyString(), any(Context.class)))
                .thenReturn(Map.of(DATA, Map.of("create_webhook", Map.of(ID, "123"))));

            WebhookEnableOutput dynamicWebhookEnableOutput = MondayNewItemInBoardTrigger.webhookEnable(
                parameters, parameters, "testWebhookUrl", "testWorkflowExecutionId", mockedTriggerContext);

            Map<String, ?> outputParameters = dynamicWebhookEnableOutput.parameters();
            LocalDateTime webhookExpirationDate = dynamicWebhookEnableOutput.webhookExpirationDate();

            assertEquals(Map.of(ID, "123"), outputParameters);
            assertNull(webhookExpirationDate);
        }
    }

    @Test
    void testDynamicWebhookRequest() {
        Map<String, Object> eventMap = new HashMap<>();

        eventMap.put("boardId", 345);
        eventMap.put("pulseId", 123);

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of("event", eventMap));

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(anyString(), any(Context.class)))
                .thenReturn(
                    Map.of(
                        DATA,
                        Map.of(
                            BOARDS, List.of(
                                Map.of(
                                    "items_page",
                                    Map.of(
                                        "items",
                                        List.of(
                                            Map.of(
                                                "column_values",
                                                List.of(
                                                    Map.of(
                                                        TYPE, "date",
                                                        TEXT, "2024-05-05", "column", Map.of(ID, "date_id")))))))))));

            Map<String, Object> result = MondayNewItemInBoardTrigger.webhookRequest(
                parameters, parameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

            Map<String, Object> expectedResult = Map.of(
                "boardId", 345, "pulseId", 123, "columnValues", Map.of("date_id", LocalDate.of(2024, 5, 5)));

            assertEquals(expectedResult, result);
        }
    }
}
