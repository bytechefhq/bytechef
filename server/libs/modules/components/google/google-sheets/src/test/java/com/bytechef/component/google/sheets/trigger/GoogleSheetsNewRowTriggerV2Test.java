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
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
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
import com.bytechef.component.google.sheets.util.GoogleSheetsRowDiffUtils;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import java.util.Arrays;
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

    private static final List<Object> ROW_1 = List.of("a1", "a2");
    private static final List<Object> ROW_2 = List.of("b1", "b2");
    private static final List<Object> ROW_3 = List.of("c1", "c2");
    private static final List<Object> INSERTED_ROW = List.of("x1", "x2");

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SPREADSHEET_ID, "123", SHEET_NAME, "abc", IS_THE_FIRST_ROW_HEADER, false));
    private final Sheets mockedSheets = mock(Sheets.class);
    private final TriggerContext.Data mockedData = mock(TriggerContext.Data.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);

    @Test
    void testWebhookRequestOnFirstRunReturnsAllRowsAsNew() {
        List<Map<String, Object>> result = executeWebhookRequest(
            List.of(ROW_1, ROW_2), Optional.empty(), List.of(ROW_1, ROW_2));

        assertEquals(List.of(mapOf(ROW_1), mapOf(ROW_2)), result);
        assertEquals(hashesOf(ROW_1, ROW_2), capturedRowHashes());
    }

    @Test
    void testWebhookRequestDetectsRowAppendedAtBottom() {
        List<Map<String, Object>> result = executeWebhookRequest(
            List.of(ROW_1, ROW_2, ROW_3), Optional.of(hashesOf(ROW_1, ROW_2)), List.of(ROW_3));

        assertEquals(List.of(mapOf(ROW_3)), result);
        assertEquals(hashesOf(ROW_1, ROW_2, ROW_3), capturedRowHashes());
    }

    @Test
    void testWebhookRequestDetectsRowInsertedInTheMiddle() {
        List<Map<String, Object>> result = executeWebhookRequest(
            List.of(ROW_1, INSERTED_ROW, ROW_2, ROW_3), Optional.of(hashesOf(ROW_1, ROW_2, ROW_3)),
            List.of(INSERTED_ROW));

        assertEquals(List.of(mapOf(INSERTED_ROW)), result);
        assertEquals(hashesOf(ROW_1, INSERTED_ROW, ROW_2, ROW_3), capturedRowHashes());
    }

    @Test
    void testWebhookRequestDoesNotTriggerWhenExistingRowIsEdited() {
        List<Object> editedRow = List.of("a1-edited", "a2-edited");

        List<Map<String, Object>> result = executeWebhookRequest(
            List.of(editedRow, ROW_2), Optional.of(hashesOf(ROW_1, ROW_2)), List.of());

        assertEquals(List.of(), result);
        assertEquals(hashesOf(editedRow, ROW_2), capturedRowHashes());
    }

    @Test
    void testWebhookRequestDoesNotTriggerWhenRowIsDeleted() {
        List<Map<String, Object>> result = executeWebhookRequest(
            List.of(ROW_1, ROW_3), Optional.of(hashesOf(ROW_1, ROW_2, ROW_3)), List.of());

        assertEquals(List.of(), result);
        assertEquals(hashesOf(ROW_1, ROW_3), capturedRowHashes());
    }

    @Test
    void testWebhookRequestSkipsHeaderRow() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(SPREADSHEET_ID, "123", SHEET_NAME, "abc", IS_THE_FIRST_ROW_HEADER, true));

        List<Map<String, Object>> result = executeWebhookRequest(
            parameters, List.of(ROW_1, ROW_2), Optional.empty(), List.of(ROW_2));

        assertEquals(List.of(mapOf(ROW_2)), result);
        assertEquals(hashesOf(ROW_2), capturedRowHashes());
    }

    @Test
    void testWebhookRequestSkipsBlankRows() {
        List<Object> blankRow = Arrays.asList("", null);

        List<Map<String, Object>> result = executeWebhookRequest(
            List.of(ROW_1, blankRow, ROW_2), Optional.empty(), List.of(ROW_1, ROW_2));

        assertEquals(List.of(mapOf(ROW_1), mapOf(ROW_2)), result);
        assertEquals(hashesOf(ROW_1, ROW_2), capturedRowHashes());
    }

    private List<Map<String, Object>> executeWebhookRequest(
        List<List<Object>> values, Optional<Object> knownRowHashes, List<List<Object>> mappedRows) {

        return executeWebhookRequest(mockedParameters, values, knownRowHashes, mappedRows);
    }

    private List<Map<String, Object>> executeWebhookRequest(
        Parameters parameters, List<List<Object>> values, Optional<Object> knownRowHashes,
        List<List<Object>> mappedRows) {

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getSheets(parameters))
                .thenReturn(mockedSheets);
            googleSheetsUtilsMockedStatic.when(() -> GoogleSheetsUtils.getSpreadsheetValues(mockedSheets, "123", "abc"))
                .thenReturn(values);

            for (List<Object> row : mappedRows) {
                googleSheetsUtilsMockedStatic
                    .when(() -> GoogleSheetsUtils.getMapOfValuesForRow(parameters, mockedSheets, row))
                    .thenReturn(mapOf(row));
            }

            when(mockedData.<Object>fetch(WORKFLOW, "knownRowHashes"))
                .thenReturn(knownRowHashes);
            when(mockedTriggerContext.data(any()))
                .thenAnswer(invocation -> {
                    ContextFunction<TriggerContext.Data, Object> function = invocation.getArgument(0);

                    return function.apply(mockedData);
                });

            return GoogleSheetsNewRowTriggerV2.webhookRequest(
                parameters, parameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
                mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> capturedRowHashes() {
        verify(mockedData).put(eq(WORKFLOW), eq("knownRowHashes"), listArgumentCaptor.capture());

        return listArgumentCaptor.getValue();
    }

    private static Map<String, Object> mapOf(List<Object> row) {
        return Map.of("row", row);
    }

    @SafeVarargs
    private static List<String> hashesOf(List<Object>... rows) {
        return Arrays.stream(rows)
            .map(GoogleSheetsRowDiffUtils::getRowHash)
            .toList();
    }
}
