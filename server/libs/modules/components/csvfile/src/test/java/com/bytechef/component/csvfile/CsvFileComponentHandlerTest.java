
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.csvfile;

import static com.bytechef.component.csvfile.constants.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.PAGE_NUMBER;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.READ_AS_STRING;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.ROWS;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class CsvFileComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);
    private static final CsvFileComponentHandler csvFileComponentHandler = new CsvFileComponentHandler();

    @Test
    public void testGetDescription() {
        JsonFileAssert.assertEquals("definition/csvfile_v1.json", csvFileComponentHandler.getDefinition());
    }

    @Test
    public void testPerformReadCSV() throws Exception {
        // headerRow: true, includeEmptyCells: false, readAsString: false

        assertEquals(
            new JSONArray(getJSONObjectsWithNamedColumns(false, false)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(true, false, null, null, false, getFile("sample_header.csv")))),
            true);

        // headerRow: true, includeEmptyCells: true, readAsString: false

        assertEquals(
            new JSONArray(getJSONObjectsWithNamedColumns(true, false)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(true, true, null, null, false, getFile("sample_header.csv")))),
            true);

        // headerRow: true, includeEmptyCells: false, readAsString: true

        assertEquals(
            new JSONArray(getJSONObjectsWithNamedColumns(false, true)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(true, false, null, null, true, getFile("sample_header.csv")))),
            true);

        // headerRow: true, includeEmptyCells: true, readAsString: true

        assertEquals(
            new JSONArray(getJSONObjectsWithNamedColumns(true, true)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(true, true, null, null, true, getFile("sample_header.csv")))),
            true);

        // headerRow: false, includeEmptyCells: false, readAsString: false

        assertEquals(
            new JSONArray(getJSONArrayWithoutNamedColumns(false, false)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(false, false, null, null, false, getFile("sample_no_header.csv")))),
            true);

        // headerRow: false, includeEmptyCells: false, readAsString: true

        assertEquals(
            new JSONArray(getJSONArrayWithoutNamedColumns(false, true)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(false, false, null, null, true, getFile("sample_no_header.csv")))),
            true);

        // headerRow: false, includeEmptyCells: true, readAsString: false

        assertEquals(
            new JSONArray(getJSONArrayWithoutNamedColumns(true, false)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(false, true, null, null, false, getFile("sample_no_header.csv")))),
            true);

        // headerRow: false, includeEmptyCells: true, readAsString: true

        assertEquals(
            new JSONArray(getJSONArrayWithoutNamedColumns(true, true)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(false, true, null, null, true, getFile("sample_no_header.csv")))),
            true);

        // paging

        assertEquals(
            new JSONArray(getJSONObjectsWithNamedColumns(false, false).subList(0, 3)),
            new JSONArray((List) csvFileComponentHandler.performRead(
                context, getReadParameters(true, false, 1, 3, false, getFile("sample_header.csv")))),
            true);
    }

    @Test
    @SuppressWarnings({
        "raw", "unchecked"
    })
    public void testPerformWriteCSV() throws IOException {
        String jsonContent = Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8);

        csvFileComponentHandler.performWrite(context, getWriteParameters((List) new JSONArray(jsonContent).toList()));

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertEquals(
            new JSONArray(jsonContent),
            new JSONArray(csvFileComponentHandler.read(inputStreamArgumentCaptor.getValue())),
            true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.csv");
    }

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

    private List<JSONObject> getJSONArrayWithoutNamedColumns(boolean includeEmptyCells, boolean readAsString)
        throws JSONException {
        return List.of(
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
        String idKey,
        Object idValue,
        String nameKey,
        String nameValue,
        String cityKey,
        String cityValue,
        String descriptionKey,
        String descriptionValue,
        String activeKey,
        Object activeValue,
        String dateKey,
        Object dateValue,
        String sumKey,
        Object sumValue)
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

    private File getFile(String fileName) {
        return new File(CsvFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + fileName)
            .getFile());
    }

    private ExecutionParameters getReadParameters(
        boolean headerRow,
        boolean includeEmptyCells,
        Integer pageNumber,
        Integer pageSize,
        boolean readAsString,
        File file)
        throws FileNotFoundException {

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getString(DELIMITER, ","))
            .thenReturn(",");
        Mockito.when(executionParameters.get(FILE_ENTRY, FileEntry.class))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(executionParameters.getBoolean(HEADER_ROW, true))
            .thenReturn(headerRow);
        Mockito.when(executionParameters.getBoolean(INCLUDE_EMPTY_CELLS, false))
            .thenReturn(includeEmptyCells);
        Mockito.when(executionParameters.getInteger(PAGE_NUMBER))
            .thenReturn(pageNumber);
        Mockito.when(executionParameters.getInteger(PAGE_SIZE))
            .thenReturn(pageSize);
        Mockito.when(executionParameters.getBoolean(READ_AS_STRING, false))
            .thenReturn(readAsString);

        if (file != null) {
            Mockito.when(context.getFileStream(Mockito.any(FileEntry.class)))
                .thenReturn(new FileInputStream(file));
        }

        return executionParameters;
    }

    @SuppressWarnings("raw")
    private ExecutionParameters getWriteParameters(List<Map> items) {
        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getList(ROWS, Map.class, List.of()))
            .thenReturn(items);

        return executionParameters;
    }
}
