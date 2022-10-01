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

package com.bytechef.component.csv.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.test.MockContext;
import com.bytechef.hermes.component.test.MockExecutionParameters;
import com.bytechef.hermes.component.test.json.JsonArrayUtils;
import com.bytechef.hermes.component.test.json.JsonObjectUtils;
import com.bytechef.hermes.test.definition.DefinitionAssert;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.assertj.core.util.Files;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class CsvFileComponentHandlerTest {

    private static final Context context = new MockContext();
    private static final CsvFileComponentHandler csvFileComponentHandler = new CsvFileComponentHandler();

    @Test
    public void testGetDescription() {
        DefinitionAssert.assertEquals("definition/csv-file_v1.json", csvFileComponentHandler.getDefinition());
    }

    @Test
    public void testPerformReadCSV() throws Exception {
        // headerRow: true, includeEmptyCells: false, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(false, false)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(true, false, null, null, false, getFile("sample_header.csv")))),
                true);

        // headerRow: true, includeEmptyCells: true, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(true, false)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(true, true, null, null, false, getFile("sample_header.csv")))),
                true);

        // headerRow: true, includeEmptyCells: false, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(false, true)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(true, false, null, null, true, getFile("sample_header.csv")))),
                true);

        // headerRow: true, includeEmptyCells: true, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(true, true)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(true, true, null, null, true, getFile("sample_header.csv")))),
                true);

        // headerRow: false, includeEmptyCells: false, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(false, false)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(false, false, null, null, false, getFile("sample_no_header.csv")))),
                true);

        // headerRow: false, includeEmptyCells: false, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(false, true)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(false, false, null, null, true, getFile("sample_no_header.csv")))),
                true);

        // headerRow: false, includeEmptyCells: true, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(true, false)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(false, true, null, null, false, getFile("sample_no_header.csv")))),
                true);

        // headerRow: false, includeEmptyCells: true, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(true, true)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(false, true, null, null, true, getFile("sample_no_header.csv")))),
                true);

        // paging

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(false, false).subList(0, 3)),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(true, false, 1, 3, false, getFile("sample_header.csv")))),
                true);
    }

    @Test
    public void testPerformWriteCSV() throws Exception {
        FileEntry fileEntry = (FileEntry) csvFileComponentHandler.performWrite(
                context,
                getWriteParameters(
                        JsonArrayUtils.toList(Files.contentOf(getFile("sample.json"), Charset.defaultCharset()))));

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List) csvFileComponentHandler.performRead(
                        context, getReadParameters(true, true, null, null, false, fileEntry))),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.csv");
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
        JSONObject jsonObject = JsonObjectUtils.of(
                idKey,
                idValue,
                nameKey,
                nameValue,
                cityKey,
                cityValue,
                activeKey,
                activeValue,
                dateKey,
                dateValue,
                sumKey,
                sumValue);

        if (descriptionValue != null) {
            jsonObject.put(descriptionKey, descriptionValue);
        }

        return jsonObject;
    }

    private File getFile(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + fileName);

        return classPathResource.getFile();
    }

    private ExecutionParameters getReadParameters(
            boolean headerRow,
            boolean includeEmptyCells,
            Integer pageNumber,
            Integer pageSize,
            boolean readAsString,
            File file)
            throws FileNotFoundException {
        return getReadParameters(
                headerRow,
                includeEmptyCells,
                pageNumber,
                pageSize,
                readAsString,
                file == null ? null : context.storeFileContent(file.getName(), new FileInputStream(file)));
    }

    private ExecutionParameters getReadParameters(
            boolean headerRow,
            boolean includeEmptyCells,
            Integer pageNumber,
            Integer pageSize,
            boolean readAsString,
            FileEntry fileEntry) {
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("fileEntry", fileEntry.toMap());
        parameters.set("headerRow", headerRow);
        parameters.set("includeEmptyCells", includeEmptyCells);
        parameters.set("pageNumber", pageNumber);
        parameters.set("pageSize", pageSize);
        parameters.set("readAsString", readAsString);

        return parameters;
    }

    private ExecutionParameters getWriteParameters(List<Object> items) {
        return new MockExecutionParameters().set("rows", items);
    }
}
