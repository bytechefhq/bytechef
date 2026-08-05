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

package com.bytechef.component.google.sheets.trigger;

import static com.bytechef.component.definition.TriggerContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Channels;
import com.google.api.services.drive.Drive.Channels.Stop;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Watch;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSheetsNewRowTriggerTest {

    private final ArgumentCaptor<Channel> channelArgumentCaptor = forClass(Channel.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final Channels mockedChannels = mock(Channels.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Files mockedFiles = mock(Files.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private Parameters mockedParameters;
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Stop mockedStop = mock(Stop.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Watch mockedWatch = mock(Watch.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);

    @Test
    void testWebhookEnable() throws IOException {
        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "spreadsheetId"));
        String workflowExecutionId = "testWorkflowExecutionId";
        String webhookUrl = "testWebhookUrl";
        UUID uuid = UUID.randomUUID();

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);

            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            when(mockedDrive.files())
                .thenReturn(mockedFiles);
            when(mockedFiles.watch(stringArgumentCaptor.capture(), channelArgumentCaptor.capture()))
                .thenReturn(mockedWatch);
            when(mockedWatch.execute())
                .thenReturn(new Channel().setId("123")
                    .setResourceId("123"));

            WebhookEnableOutput result = GoogleSheetsNewRowTrigger.webhookEnable(
                mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

            WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
                Map.of("id", "123", "resourceId", "123"), null);

            assertEquals(expectedWebhookEnableOutput, result);

            Channel channel = new Channel()
                .setAddress(webhookUrl)
                .setId(String.valueOf(uuid))
                .setPayload(true)
                .setType("web_hook");

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(channel, channelArgumentCaptor.getValue());
            assertEquals("spreadsheetId", stringArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable() throws IOException {
        String workflowExecutionId = "testWorkflowExecutionId";
        mockedParameters = MockParametersFactory.create(Map.of("id", "123", "resourceId", "abc"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getDrive(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDrive);

            when(mockedDrive.channels())
                .thenReturn(mockedChannels);
            when(mockedChannels.stop(channelArgumentCaptor.capture()))
                .thenReturn(mockedStop);

            GoogleSheetsNewRowTrigger.webhookDisable(
                mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

            Channel expectedChannel = new Channel()
                .setId("123")
                .setResourceId("abc");

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(expectedChannel, channelArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWebhookRequestOnFirstRunReturnsAllRowsAsNew() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "123", SHEET_NAME, "abc"));

        List<Object> row1 = List.of("a1", "a2");
        List<Object> row2 = List.of("b1", "b2");

        TriggerContext.Data mockedData = mock(TriggerContext.Data.class);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "123", "abc"))
                .thenReturn(List.of(row1, row2));
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row1))
                .thenReturn(Map.of("col1", "a1"));
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row2))
                .thenReturn(Map.of("col1", "b1"));

            when(mockedData.<Object>fetch(WORKFLOW, "knownRowHashes"))
                .thenReturn(Optional.empty());
            when(mockedTriggerContext.data(any()))
                .thenAnswer(invocation -> {
                    ContextFunction<TriggerContext.Data, Object> function = invocation.getArgument(0);

                    return function.apply(mockedData);
                });

            List<Map<String, Object>> result = GoogleSheetsNewRowTrigger.webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            assertEquals(List.of(Map.of("col1", "a1"), Map.of("col1", "b1")), result);

            verify(mockedData).put(eq(WORKFLOW), eq("knownRowHashes"), listArgumentCaptor.capture());

            assertEquals(List.of(row1.hashCode(), row2.hashCode()), listArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWebhookRequestDetectsRowInsertedInTheMiddle() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "123", SHEET_NAME, "abc"));

        // rowC is inserted between rowA and rowB, so the sheet no longer just grew at the bottom
        List<Object> rowA = List.of("a1", "a2");
        List<Object> rowB = List.of("b1", "b2");
        List<Object> rowC = List.of("c1", "c2");

        TriggerContext.Data mockedData = mock(TriggerContext.Data.class);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "123", "abc"))
                .thenReturn(List.of(rowA, rowC, rowB));
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, rowC))
                .thenReturn(Map.of("col1", "c1"));

            when(mockedData.<Object>fetch(WORKFLOW, "knownRowHashes"))
                .thenReturn(Optional.of(List.of(rowA.hashCode(), rowB.hashCode())));
            when(mockedTriggerContext.data(any()))
                .thenAnswer(invocation -> {
                    ContextFunction<TriggerContext.Data, Object> function = invocation.getArgument(0);

                    return function.apply(mockedData);
                });

            List<Map<String, Object>> result = GoogleSheetsNewRowTrigger.webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            // Only rowC is genuinely new; the old position-based logic would have wrongly
            // returned rowB here, since rowB now sits in the last position.
            assertEquals(List.of(Map.of("col1", "c1")), result);

            verify(mockedData).put(eq(WORKFLOW), eq("knownRowHashes"), listArgumentCaptor.capture());

            assertEquals(
                List.of(rowA.hashCode(), rowC.hashCode(), rowB.hashCode()), listArgumentCaptor.getValue());
        }
    }
}
