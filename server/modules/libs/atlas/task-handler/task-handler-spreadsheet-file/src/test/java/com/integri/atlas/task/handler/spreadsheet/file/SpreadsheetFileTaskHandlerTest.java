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

package com.integri.atlas.task.handler.spreadsheet.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import com.integri.atlas.test.json.JSONArrayUtil;
import com.integri.atlas.test.json.JSONObjectUtil;
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
public class SpreadsheetFileTaskHandlerTest {

    private static final FileStorageService fileStorageService = new Base64FileStorageService();
    private static final JSONHelper jsonHelper = new JSONHelper(new ObjectMapper());
    private static final SpreadsheetFileTaskHandler spreadsheetFileTaskHandler = new SpreadsheetFileTaskHandler(
        jsonHelper,
        fileStorageService
    );

    @Test
    public void testReadCSV() throws Exception {
        readFile("csv");
    }

    @Test
    public void testReadODS() throws Exception {
        readFile("ods");
    }

    @Test
    public void testReadXLS() throws Exception {
        readFile("xls");
    }

    @Test
    public void testReadXLSX() throws Exception {
        readFile("xlsx");
    }

    @Test
    public void testWriteCSV() throws Exception {
        writeFile("csv");
    }

    @Test
    public void testWriteODS() throws Exception {
        writeFile("ods");
    }

    @Test
    public void testWriteXLS() throws Exception {
        writeFile("xls");
    }

    @Test
    public void testWriteXLSX() throws Exception {
        writeFile("xlsx");
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
                readAsString ? "11.2" : 11.2
            ),
            getJSONObjectWithNamedColumns(
                readAsString ? "4" : 4,
                "name1",
                "city1",
                "description1",
                readAsString ? "false" : false,
                includeEmptyCells ? "" : null,
                readAsString ? "12" : 12
            ),
            getJSONObjectWithNamedColumns(
                readAsString ? "2" : 2,
                "A",
                "city2",
                includeEmptyCells ? "" : null,
                readAsString ? "true" : true,
                "2021-12-09",
                includeEmptyCells ? "" : null
            ),
            getJSONObjectWithNamedColumns(
                readAsString ? "5678" : 5678,
                "ABCD",
                "city3",
                "EFGH",
                readAsString ? "false" : false,
                "2021-12-10",
                readAsString ? "13.23" : 13.23
            )
        );
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
                readAsString ? "11.2" : 11.2
            ),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "4" : 4,
                "name1",
                "city1",
                "description1",
                readAsString ? "false" : false,
                includeEmptyCells ? "" : null,
                readAsString ? "12" : 12
            ),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "2" : 2,
                "A",
                "city2",
                includeEmptyCells ? "" : null,
                readAsString ? "true" : true,
                "2021-12-09",
                includeEmptyCells ? "" : null
            ),
            getJSONObjectWithoutNamedColumns(
                readAsString ? "5678" : 5678,
                "ABCD",
                "city3",
                "EFGH",
                readAsString ? "false" : false,
                "2021-12-10",
                readAsString ? "13.23" : 13.23
            )
        );
    }

    private JSONObject getJSONObjectWithNamedColumns(
        Object id,
        String name,
        String city,
        String description,
        Object active,
        String date,
        Object sum
    ) throws JSONException {
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
            sum
        );
    }

    private JSONObject getJSONObjectWithoutNamedColumns(
        Object id,
        String name,
        String city,
        String description,
        Object active,
        String date,
        Object sum
    ) throws JSONException {
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
            sum
        );
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
        Object sumValue
    ) throws JSONException {
        JSONObject jsonObject = JSONObjectUtil.of(
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
            sumValue
        );

        if (descriptionValue != null) {
            jsonObject.put(descriptionKey, descriptionValue);
        }

        return jsonObject;
    }

    private File getFile(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + fileName);

        return classPathResource.getFile();
    }

    private SimpleTaskExecution getReadSimpleTaskExecution(
        boolean headerRow,
        boolean includeEmptyCells,
        Integer pageNumber,
        Integer pageSize,
        boolean readAsString,
        File file
    ) throws FileNotFoundException {
        return getReadSimpleTaskExecution(
            headerRow,
            includeEmptyCells,
            pageNumber,
            pageSize,
            readAsString,
            file == null ? null : fileStorageService.storeFileContent(file.getName(), new FileInputStream(file))
        );
    }

    private SimpleTaskExecution getReadSimpleTaskExecution(
        boolean headerRow,
        boolean includeEmptyCells,
        Integer pageNumber,
        Integer pageSize,
        boolean readAsString,
        FileEntry fileEntry
    ) {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileEntry", fileEntry);
        taskExecution.put("headerRow", headerRow);
        taskExecution.put("includeEmptyCells", includeEmptyCells);
        taskExecution.put("operation", "READ");
        taskExecution.put("pageNumber", pageNumber);
        taskExecution.put("pageSize", pageSize);
        taskExecution.put("readAsString", readAsString);

        return taskExecution;
    }

    private SimpleTaskExecution getWriteSimpleTaskExecution(String fileFormat, List<Object> input) {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put("fileFormat", fileFormat);
        taskExecution.put("input", input);
        taskExecution.put("operation", "WRITE");

        return taskExecution;
    }

    private void readFile(String extension) throws Exception {
        //headerRow: true, includeEmptyCells: false, readAsString: false

        assertEquals(
            JSONArrayUtil.of(getJSONObjectsWithNamedColumns(false, false)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(true, false, null, null, false, getFile("sample_header." + extension))
                )
            ),
            true
        );

        //headerRow: true, includeEmptyCells: true, readAsString: false

        assertEquals(
            JSONArrayUtil.of(getJSONObjectsWithNamedColumns(true, false)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(true, true, null, null, false, getFile("sample_header." + extension))
                )
            ),
            true
        );

        //headerRow: true, includeEmptyCells: false, readAsString: true

        assertEquals(
            JSONArrayUtil.of(getJSONObjectsWithNamedColumns(false, true)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(true, false, null, null, true, getFile("sample_header." + extension))
                )
            ),
            true
        );

        //headerRow: true, includeEmptyCells: true, readAsString: true

        assertEquals(
            JSONArrayUtil.of(getJSONObjectsWithNamedColumns(true, true)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(true, true, null, null, true, getFile("sample_header." + extension))
                )
            ),
            true
        );

        //headerRow: false, includeEmptyCells: false, readAsString: false

        assertEquals(
            JSONArrayUtil.of(getJSONArrayWithoutNamedColumns(false, false)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(
                        false,
                        false,
                        null,
                        null,
                        false,
                        getFile("sample_no_header." + extension)
                    )
                )
            ),
            true
        );

        //headerRow: false, includeEmptyCells: false, readAsString: true

        assertEquals(
            JSONArrayUtil.of(getJSONArrayWithoutNamedColumns(false, true)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(false, false, null, null, true, getFile("sample_no_header." + extension))
                )
            ),
            true
        );

        //headerRow: false, includeEmptyCells: true, readAsString: false

        assertEquals(
            JSONArrayUtil.of(getJSONArrayWithoutNamedColumns(true, false)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(false, true, null, null, false, getFile("sample_no_header." + extension))
                )
            ),
            true
        );

        //headerRow: false, includeEmptyCells: true, readAsString: true

        assertEquals(
            JSONArrayUtil.of(getJSONArrayWithoutNamedColumns(true, true)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(false, true, null, null, true, getFile("sample_no_header." + extension))
                )
            ),
            true
        );

        //paging

        assertEquals(
            JSONArrayUtil.of(getJSONObjectsWithNamedColumns(false, false).subList(0, 3)),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(true, false, 1, 3, false, getFile("sample_header." + extension))
                )
            ),
            true
        );
    }

    private void writeFile(String fileFormat) throws Exception {
        FileEntry fileEntry = (FileEntry) spreadsheetFileTaskHandler.handle(
            getWriteSimpleTaskExecution(
                fileFormat,
                JSONArrayUtil.toList(Files.contentOf(getFile("sample.json"), Charset.defaultCharset()))
            )
        );

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
            JSONArrayUtil.of(
                (List<?>) spreadsheetFileTaskHandler.handle(
                    getReadSimpleTaskExecution(true, true, null, null, false, fileEntry)
                )
            ),
            true
        );

        assertThat(fileEntry.getName()).isEqualTo("spreadsheet." + fileFormat);
    }
}
