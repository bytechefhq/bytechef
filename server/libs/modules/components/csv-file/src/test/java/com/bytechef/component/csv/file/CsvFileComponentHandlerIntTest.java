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

package com.bytechef.component.csv.file;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ENCLOSING_CHARACTER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.component.test.ComponentJobTestExecutor;
import com.bytechef.platform.component.test.annotation.ComponentIntTest;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@ComponentIntTest
class CsvFileComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Autowired
    private TempFileStorage tempFileStorage;

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Test
    void testRead() throws JSONException {
        File sampleFile = getFile("sample.csv");

        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
                        DELIMITER, ",", HEADER_ROW, false,
                        INCLUDE_EMPTY_CELLS, true));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        JSONArray expectedJSONArray = new JSONArray(
                Files.contentOf(getFile("expected_output.json"), StandardCharsets.UTF_8));

        assertEquals(
                expectedJSONArray,
                new JSONArray((List<?>) outputs.get("readCsvFile")),
                true);
    }

    @Test
    void testReadHeader() throws JSONException {
        File sampleFile = getFile("sample_header.csv");

        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
                        DELIMITER, ",",
                        HEADER_ROW, true,
                        INCLUDE_EMPTY_CELLS, true));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertEquals(
                new JSONArray(Files.contentOf(getFile("expected_output_header.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) outputs.get("readCsvFile")),
                true);
    }

    @Test
    void testReadHeaderAndQuoted() throws JSONException {
        File sampleFile = getFile("sample_header_quoted.csv");

        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
                        DELIMITER, ",",
                        HEADER_ROW, true,
                        INCLUDE_EMPTY_CELLS, true,
                        ENCLOSING_CHARACTER, "'"));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertEquals(
                new JSONArray(Files.contentOf(getFile("expected_output_header.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) outputs.get("readCsvFile")),
                true);
    }

    @Test
    void testReadHeaderAndDelimiter() throws JSONException {
        File sampleFile = getFile("sample_header_pipe_delimiter.csv");

        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
                        INCLUDE_EMPTY_CELLS, true, DELIMITER, "|",
                        HEADER_ROW, true));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertEquals(
                new JSONArray(Files.contentOf(getFile("expected_output_header.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) outputs.get("readCsvFile")),
                true);
    }

    @Test
    void testReadHeaderAndDelimiterAdvanced() throws JSONException {
        File sampleFile = getFile("sample_header_semicolon_delimiter.csv");

        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
                        INCLUDE_EMPTY_CELLS, true, DELIMITER, ";",
                        HEADER_ROW, true));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertThat(((Map<?, ?>) ((List<?>) outputs.get("readCsvFile")).getFirst()).size())
                .isEqualTo(12);
    }

    @Test
    void testReadNoHeaderWithDelimiter() throws JSONException {
        File sampleFile = getFile("sample_no_header_semicolon_delimiter.csv");

        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
                        INCLUDE_EMPTY_CELLS, true, DELIMITER, ";",
                        HEADER_ROW, false));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertThat(((Map<?, ?>) ((List<?>) outputs.get("readCsvFile")).getFirst()).size())
                .isEqualTo(12);
    }

    // @Test
    public void testWrite() throws JSONException {
        Job job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_write".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        "rows",
                        new JSONArray(Files.contentOf(getFile("expected_output.json"), StandardCharsets.UTF_8))
                                .toList()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertThat(((Map) outputs.get("writeCsvFile")).get(WorkflowConstants.NAME))
                .isEqualTo("file.csv");

        File sampleFile = getFile("sample_header.csv");

        job = componentJobTestExecutor.execute(
                ENCODER.encodeToString("csv-file_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                        FILE_ENTRY,
                        tempFileStorage.storeFileContent(
                                sampleFile.getName(), Files.contentOf(sampleFile, StandardCharsets.UTF_8))));

        outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertEquals(
                new JSONArray(Files.contentOf(getFile("expected_output.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) outputs.get("readCsvFile")),
                true);
    }

    private File getFile(String fileName) {
        return new File(CsvFileComponentHandlerIntTest.class
                .getClassLoader()
                .getResource("dependencies/csv-file/" + fileName)
                .getFile());
    }
}
