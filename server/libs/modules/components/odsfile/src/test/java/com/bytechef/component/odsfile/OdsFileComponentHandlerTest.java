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

package com.bytechef.component.odsfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.test.json.JsonArrayUtils;
import com.bytechef.hermes.component.test.json.JsonObjectUtils;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.jsonasssert.AssertUtils;
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
public class OdsFileComponentHandlerTest {

    private static final MockContext context = new MockContext();
    private static final OdsFileComponentHandler odsFileComponentHandler = new OdsFileComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        AssertUtils.assertEquals("definition/odsfile_v1.json", new OdsFileComponentHandler().getDefinition());
    }

    @Test
    public void testPerformReadODS() throws IOException, JSONException {
        // headerRow: true, includeEmptyCells: false, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(false, false)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(true, false, null, null, false, getFile("sample_header.ods")))),
                true);

        // headerRow: true, includeEmptyCells: true, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(true, false)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(true, true, null, null, false, getFile("sample_header.ods")))),
                true);

        // headerRow: true, includeEmptyCells: false, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(false, true)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(true, false, null, null, true, getFile("sample_header.ods")))),
                true);

        // headerRow: true, includeEmptyCells: true, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(true, true)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(true, true, null, null, true, getFile("sample_header.ods")))),
                true);

        // headerRow: false, includeEmptyCells: false, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(false, false)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(false, false, null, null, false, getFile("sample_no_header.ods")))),
                true);

        // headerRow: false, includeEmptyCells: false, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(false, true)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(false, false, null, null, true, getFile("sample_no_header.ods")))),
                true);

        // headerRow: false, includeEmptyCells: true, readAsString: false

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(true, false)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(false, true, null, null, false, getFile("sample_no_header.ods")))),
                true);

        // headerRow: false, includeEmptyCells: true, readAsString: true

        assertEquals(
                JsonArrayUtils.of(getJSONArrayWithoutNamedColumns(true, true)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(false, true, null, null, true, getFile("sample_no_header.ods")))),
                true);

        // paging

        assertEquals(
                JsonArrayUtils.of(getJSONObjectsWithNamedColumns(false, false).subList(0, 3)),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(true, false, 1, 3, false, getFile("sample_header.ods")))),
                true);
    }

    @Test
    public void testPerformWriteODS() throws IOException, JSONException {
        String jsonContent = Files.contentOf(getFile("sample.json"), Charset.defaultCharset());

        assertThat(jsonContent).isNotNull();

        ExecutionParameters executionParameters = getWriteParameters(JsonArrayUtils.toList(jsonContent));

        assertThat(executionParameters).isNotNull();

        FileEntry fileEntry = (FileEntry) odsFileComponentHandler.performWrite(context, executionParameters);

        assertThat(fileEntry).isNotNull();
        assertThat(fileEntry.getName()).isNotNull();

        assertEquals(
                JsonArrayUtils.of(jsonContent),
                JsonArrayUtils.of((List) odsFileComponentHandler.performRead(
                        context, getReadParameters(true, true, null, null, false, fileEntry))),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.ods");
    }

    private List<JSONObject> getJSONObjectsWithNamedColumns(boolean includeEmptyCells, boolean readAsString)
            throws JSONException {
        return List.of(
                getJSONObjectWithNamedColumns(
                        readAsString ? "77.0" : 77,
                        "A",
                        "B",
                        "C",
                        readAsString ? "true" : true,
                        "2021-12-07",
                        readAsString ? "11.2" : 11.2),
                getJSONObjectWithNamedColumns(
                        readAsString ? "4.0" : 4,
                        "name1",
                        "city1",
                        "description1",
                        readAsString ? "false" : false,
                        includeEmptyCells ? "" : null,
                        readAsString ? "12.0" : 12),
                getJSONObjectWithNamedColumns(
                        readAsString ? "2.0" : 2,
                        "A",
                        "city2",
                        includeEmptyCells ? "" : null,
                        readAsString ? "true" : true,
                        "2021-12-09",
                        includeEmptyCells ? "" : null),
                getJSONObjectWithNamedColumns(
                        readAsString ? "5678.0" : 5678,
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
                        readAsString ? "77.0" : 77,
                        "A",
                        "B",
                        "C",
                        readAsString ? "true" : true,
                        "2021-12-07",
                        readAsString ? "11.2" : 11.2),
                getJSONObjectWithoutNamedColumns(
                        readAsString ? "4.0" : 4,
                        "name1",
                        "city1",
                        "description1",
                        readAsString ? "false" : false,
                        includeEmptyCells ? "" : null,
                        readAsString ? "12.0" : 12),
                getJSONObjectWithoutNamedColumns(
                        readAsString ? "2.0" : 2,
                        "A",
                        "city2",
                        includeEmptyCells ? "" : null,
                        readAsString ? "true" : true,
                        "2021-12-09",
                        includeEmptyCells ? "" : null),
                getJSONObjectWithoutNamedColumns(
                        readAsString ? "5678.0" : 5678,
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
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("rows", items);

        return parameters;
    }
}
