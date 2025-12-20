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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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

    private final ArgumentCaptor<Channel> channelArgumentCaptor = ArgumentCaptor.forClass(Channel.class);
    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Drive.Channels mockedChannels = mock(Drive.Channels.class);
    private final Drive mockedDrive = mock(Drive.class);
    private final Drive.Files mockedFiles = mock(Drive.Files.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private Parameters mockedParameters;
    private final Sheets mockedSheets = mock(Sheets.class);
    private final Drive.Channels.Stop mockedStop = mock(Drive.Channels.Stop.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Drive.Files.Watch mockedWatch = mock(Drive.Files.Watch.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<Sheets> sheetsArgumentCaptor = ArgumentCaptor.forClass(Sheets.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

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
    void testWebhookRequest() throws Exception {
        mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "123", SHEET_NAME, "abc"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getSheets(parametersArgumentCaptor.capture()))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getSpreadsheetValues(
                    sheetsArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(List.of(List.of()));
            googleServicesMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRowAndColumn(
                    parametersArgumentCaptor.capture(), sheetsArgumentCaptor.capture(), listArgumentCaptor.capture(),
                    integerArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn(List.of());

            when(mockedTriggerContext.data(any()))
                .thenReturn(Optional.of(123));

            List<Map<String, Object>> result = GoogleSheetsNewRowTrigger.webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            assertEquals(List.of(), result);

            assertEquals(List.of(mockedSheets, mockedSheets), sheetsArgumentCaptor.getAllValues());
            assertEquals(List.of("123", "abc"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(123, 1), integerArgumentCaptor.getAllValues());
        }
    }
}
