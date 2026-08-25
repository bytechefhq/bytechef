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
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Anshul Goel
 */
class GoogleSheetsNewRowTriggerV2Test {

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(SPREADSHEET_ID, "123", SHEET_NAME, "abc"));
    private final Sheets mockedSheets = mock(Sheets.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);

    @Test
    @SuppressWarnings("unchecked")
    void testWebhookRequestOnFirstRunReturnsAllRowsAsNew() {
        List<Object> row1 = List.of("a1", "a2");
        List<Object> row2 = List.of("b1", "b2");
        TriggerContext.Data mockedData = mock(TriggerContext.Data.class);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic.when(() -> GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "123", "abc"))
                .thenReturn(List.of(row1, row2));
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row1))
                .thenReturn(Map.of("col1", "a1"));
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row2))
                .thenReturn(Map.of("col1", "b1"));

            when(mockedData.<Object>fetch(WORKFLOW, "knownRowIds"))
                .thenReturn(Optional.empty());
            when(mockedTriggerContext.data(any()))
                .thenAnswer(invocation -> {
                    ContextFunction<TriggerContext.Data, Object> function = invocation.getArgument(0);

                    return function.apply(mockedData);
                });

            List<Map<String, Object>> result = GoogleSheetsNewRowTriggerV2.webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            assertEquals(List.of(Map.of("col1", "a1"), Map.of("col1", "b1")), result);

            verify(mockedData).put(eq(WORKFLOW), eq("knownRowIds"), listArgumentCaptor.capture());

            assertEquals(List.of(1, 2), listArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWebhookRequestDetectsRowAppendedAtBottom() {
        List<Object> row1 = List.of("a1", "a2");
        List<Object> row2 = List.of("b1", "b2");
        List<Object> row3 = List.of("c1", "c2");
        TriggerContext.Data mockedData = mock(TriggerContext.Data.class);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic.when(() -> GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "123", "abc"))
                .thenReturn(List.of(row1, row2, row3));
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(mockedParameters, mockedSheets, row3))
                .thenReturn(Map.of("col1", "c1"));

            when(mockedData.<Object>fetch(WORKFLOW, "knownRowIds"))
                .thenReturn(Optional.of(List.of(1, 2)));
            when(mockedTriggerContext.data(any()))
                .thenAnswer(invocation -> {
                    ContextFunction<TriggerContext.Data, Object> function = invocation.getArgument(0);

                    return function.apply(mockedData);
                });

            List<Map<String, Object>> result = GoogleSheetsNewRowTriggerV2.webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            assertEquals(List.of(Map.of("col1", "c1")), result);

            verify(mockedData).put(eq(WORKFLOW), eq("knownRowIds"), listArgumentCaptor.capture());

            assertEquals(List.of(1, 2, 3), listArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWebhookRequestDoesNotTriggerWhenExistingRowIsEdited() {
        List<Object> editedRow = List.of("a1-edited", "a2-edited");
        TriggerContext.Data mockedData = mock(TriggerContext.Data.class);

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(mockedParameters))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic.when(() -> GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "123", "abc"))
                .thenReturn(List.of(editedRow));

            when(mockedData.<Object>fetch(WORKFLOW, "knownRowIds"))
                .thenReturn(Optional.of(List.of(1)));
            when(mockedTriggerContext.data(any()))
                .thenAnswer(invocation -> {
                    ContextFunction<TriggerContext.Data, Object> function = invocation.getArgument(0);

                    return function.apply(mockedData);
                });

            List<Map<String, Object>> result = GoogleSheetsNewRowTriggerV2.webhookRequest(
                mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

            assertEquals(List.of(), result);

            verify(mockedData).put(eq(WORKFLOW), eq("knownRowIds"), listArgumentCaptor.capture());

            assertEquals(List.of(1), listArgumentCaptor.getValue());
        }
    }
}
