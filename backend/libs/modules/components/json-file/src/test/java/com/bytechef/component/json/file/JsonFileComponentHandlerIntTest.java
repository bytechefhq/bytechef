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

package com.bytechef.component.json.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.test.workflow.WorkflowExecutor;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.test.MockFileEntry;
import com.bytechef.hermes.component.test.json.JsonArrayUtils;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class JsonFileComponentHandlerIntTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @Test
    public void testRead() throws IOException, JSONException {
        File sampleFile = getFile("sample_array.json");

        Job job = workflowExecutor.execute(
                "json-file_v1_read",
                Map.of(
                        "fileEntry",
                        fileStorageService
                                .storeFileContent(
                                        sampleFile.getAbsolutePath(),
                                        Files.contentOf(sampleFile, Charset.defaultCharset()))
                                .toMap()));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        JSONAssert.assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) outputs.get("readJSONFile")),
                true);
    }

    @Test
    public void testWrite() throws IOException, JSONException {
        Job job = workflowExecutor.execute(
                "json-file_v1_write",
                Map.of(
                        "source",
                        JsonArrayUtils.toList(
                                Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset()))));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        FileEntry fileEntry = new MockFileEntry(outputs, "writeJSONFile");
        File sampleFile = getFile("sample_array.json");

        job = workflowExecutor.execute(
                "json-file_v1_read",
                Map.of(
                        "fileEntry",
                        fileStorageService
                                .storeFileContent(
                                        sampleFile.getName(), Files.contentOf(sampleFile, Charset.defaultCharset()))
                                .toMap()));

        outputs = job.getOutputs();

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(sampleFile, Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) outputs.get("readJSONFile")),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.json");
    }

    private File getFile(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + fileName);

        return classPathResource.getFile();
    }
}
