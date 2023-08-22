
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.hermes.component.test.JobTestExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class CsvFileComponentHandlerIntTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JobTestExecutor jobTestExecutor;

    @Test
    public void testRead() throws JSONException {
        File sampleFile = getFile("sample_header.csv");

        Job job = jobTestExecutor.execute(
            Base64.getEncoder()
                .encodeToString("csvfile_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        sampleFile.getAbsolutePath(),
                        Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = job.getOutputs();

        assertEquals(
            new JSONArray(Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8)),
            new JSONArray((List<?>) outputs.get("readCsvFile")),
            true);
    }

    @Test
    public void testWrite() throws JSONException {
        Job job = jobTestExecutor.execute(
            Base64.getEncoder()
                .encodeToString("csvfile_v1_write".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "rows",
                new JSONArray(Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8)).toList()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = job.getOutputs();

        assertThat(((Map) outputs.get("writeCsvFile")).get(WorkflowConstants.NAME))
            .isEqualTo("file.csv");

        File sampleFile = getFile("sample_header.csv");

        job = jobTestExecutor.execute(
            Base64.getEncoder()
                .encodeToString("csvfile_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        sampleFile.getName(), Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        outputs = job.getOutputs();

        assertEquals(
            new JSONArray(Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8)),
            new JSONArray((List<?>) outputs.get("readCsvFile")),
            true);
    }

    private File getFile(String fileName) {
        return new File(CsvFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + fileName)
            .getFile());
    }
}
