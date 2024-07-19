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

package com.bytechef.component.csv.file.action;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_NUMBER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.READ_AS_STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.component.csv.file.CsvFileComponentHandlerTest;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
//@Disabled
class CsvFileReadActionTest {
    private static final ActionContext context = mock(ActionContext.class);
    private final Parameters parameters = mock(Parameters.class);


    @Test
    void testPerformReadCSVHeaderRow() throws Exception {
        testPerformReadCSV(true, false, false);
    }

    @Test
    void testPerformReadCSVHeaderRowIncludeEmptyCells() throws Exception {
        testPerformReadCSV(true, true, false);
    }

    @Test
    void testPerformReadCSVHeaderRowReadAsString() throws Exception {
        testPerformReadCSV(true, false, true);
    }

    @Test
    void testPerformReadCSVHeaderRowIncludeEmptyCellsReadAsString() throws Exception {
        testPerformReadCSV(true, true, true);
    }

    @Test
    void testPerformReadCSV() throws Exception {
        testPerformReadCSV(false, false, false);
    }

    @Test
    void testPerformReadCSVReadAsString() throws Exception {
        testPerformReadCSV(false, false, true);
    }

    @Test
    void testPerformReadCSVIncludeEmptyCells() throws Exception {
        testPerformReadCSV(false, true, false);
    }

    @Test
    void testPerformReadCSVIncludeEmptyCellsReadAsString() throws Exception {
        testPerformReadCSV(false, true, true);
    }

    private void testPerformReadCSV(boolean headerRow, boolean includeEmptyCells, boolean readAsString) throws IOException {
        List<JSONObject> jsonObjects = null;
        if(headerRow) jsonObjects = getJSONObjectsWithNamedColumns(includeEmptyCells, readAsString);
        else jsonObjects = getJSONArrayWithoutNamedColumns(includeEmptyCells, readAsString);
        JSONArray expected = new JSONArray(jsonObjects);

        Parameters mockedParameters = getReadParameters(headerRow, includeEmptyCells, null, null, readAsString, getFile("sample_header.csv"), parameters);

        JSONArray result = new JSONArray(CsvFileReadAction.perform(mockedParameters, parameters, context));

        assertEquals(expected, result, true);
    }

    @Test
    void testPerformReadCSVPaging() throws Exception {
        JSONArray expected = new JSONArray(getJSONObjectsWithNamedColumns(false, false).subList(0, 3));

        Parameters mockedParameters = getReadParameters(true, false, 1, 3, false, getFile("sample_header.csv"), parameters);

        JSONArray result = new JSONArray(CsvFileReadAction.perform(mockedParameters, parameters, context));

        assertEquals(expected, result, true);
    }


    @SuppressWarnings("PMD.SimplifiedTernary")
    private List<JSONObject> getJSONObjectsWithNamedColumns(boolean includeEmptyCells, boolean readAsString)
        throws JSONException {
        return List.of(
            getJSONObjectWithNamedColumns(
                readAsString ? "77" : 77,
                "A",
                "B",
                "C",
                readAsString ? "true" : true,
                "2021-12-07",
                readAsString ? "11.2" : 11.2),
            getJSONObjectWithNamedColumns(
                readAsString ? "4" : 4,
                "name1",
                "city1",
                "description1",
                readAsString ? "false" : false,
                includeEmptyCells ? "" : null,
                readAsString ? "12" : 12),
            getJSONObjectWithNamedColumns(
                readAsString ? "2" : 2,
                "A",
                "city2",
                includeEmptyCells ? "" : null,
                readAsString ? "true" : true,
                "2021-12-09",
                includeEmptyCells ? "" : null),
            getJSONObjectWithNamedColumns(
                readAsString ? "5678" : 5678,
                "ABCD",
                "city3",
                "EFGH",
                readAsString ? "false" : false,
                "2021-12-10",
                readAsString ? "13.23" : 13.23));
    }

    @SuppressWarnings("PMD.SimplifiedTernary")
    private List<JSONObject> getJSONArrayWithoutNamedColumns(boolean includeEmptyCells, boolean readAsString)
        throws JSONException {
        return List.of(
            getJSONObjectWithoutNamedColumns(
                "id",
                "name",
                "city",
                "description",
                "active",
                "date",
                "sum"),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "77" : 77,
                "A",
                "B",
                "C",
                readAsString ? "true" : true,
                "2021-12-07",
                readAsString ? "11.2" : 11.2),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "4" : 4,
                "name1",
                "city1",
                "description1",
                readAsString ? "false" : false,
                includeEmptyCells ? "" : null,
                readAsString ? "12" : 12),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "2" : 2,
                "A",
                "city2",
                includeEmptyCells ? "" : null,
                readAsString ? "true" : true,
                "2021-12-09",
                includeEmptyCells ? "" : null),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "5678" : 5678,
                "ABCD",
                "city3",
                "EFGH",
                readAsString ? "false" : false,
                "2021-12-10",
                readAsString ? "13.23" : 13.23));
    }

    private JSONObject getJSONObjectWithNamedColumns(
        Object id, String name, String city, String description, Object active, String date, Object sum)
        throws JSONException {
        return getJSONObject(
            "id",
            id,
            "name",
            name,
            "city",
            city,
            "description",
            description,
            "active",
            active,
            "date",
            date,
            "sum",
            sum);
    }

    private JSONObject getJSONObjectWithoutNamedColumns(
        Object id, String name, String city, String description, Object active, String date, Object sum)
        throws JSONException {
        return getJSONObject(
            "column_1",
            id,
            "column_2",
            name,
            "column_3",
            city,
            "column_4",
            description,
            "column_5",
            active,
            "column_6",
            date,
            "column_7",
            sum);
    }

    private JSONObject getJSONObject(
        String idKey, Object idValue, String nameKey, String nameValue, String cityKey, String cityValue,
        String descriptionKey, String descriptionValue, String activeKey, Object activeValue, String dateKey,
        Object dateValue, String sumKey, Object sumValue)
        throws JSONException {
        JSONObject jsonObject = new JSONObject()
            .put(idKey, idValue)
            .put(nameKey, nameValue)
            .put(cityKey, cityValue)
            .put(activeKey, activeValue)
            .put(dateKey, dateValue)
            .put(sumKey, sumValue);

        if (descriptionValue != null) {
            jsonObject.put(descriptionKey, descriptionValue);
        }

        return jsonObject;
    }

    @SuppressFBWarnings("OBL")
    private Parameters getReadParameters(
        boolean headerRow, boolean includeEmptyCells, Integer pageNumber, Integer pageSize, boolean readAsString,
        File file, Parameters parameters)
        throws FileNotFoundException {

        when(parameters.getString(DELIMITER, ","))
            .thenReturn(",");
        when(parameters.getRequiredFileEntry(FILE_ENTRY))
            .thenReturn(mock(FileEntry.class));
        when(parameters.getBoolean(HEADER_ROW, true))
            .thenReturn(headerRow);
        when(parameters.getBoolean(INCLUDE_EMPTY_CELLS, false))
            .thenReturn(includeEmptyCells);
        when(parameters.getInteger(PAGE_NUMBER))
            .thenReturn(pageNumber);
        when(parameters.getInteger(PAGE_SIZE))
            .thenReturn(pageSize);
        when(parameters.getBoolean(READ_AS_STRING, false))
            .thenReturn(readAsString);

        if (file != null) {
            when(context.file(any()))
                .thenReturn(new FileInputStream(file));
        }

        return parameters;
    }

    private File getFile(String fileName) {
        return new File(Objects.requireNonNull(CsvFileComponentHandlerTest.class
                        .getClassLoader()
                        .getResource("dependencies/csv-file/" + fileName))
            .getFile());
    }
}
