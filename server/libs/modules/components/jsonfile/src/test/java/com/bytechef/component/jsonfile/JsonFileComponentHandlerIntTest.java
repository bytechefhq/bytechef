
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

package com.bytechef.component.jsonfile;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.component.jsonfile.util.JsonStreamUtils;
import com.bytechef.hermes.component.test.workflow.ComponentWorkflowTestSupport;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.file.storage.service.FileStorageService;
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
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@Import(JsonStreamUtils.class)
@ComponentIntTest
public class JsonFileComponentHandlerIntTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ComponentWorkflowTestSupport componentWorkflowTestSupport;

    @Test
    public void testRead() throws JSONException {
        File sampleFile = getFile("sample_array.json");

        Job job = componentWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("jsonfile_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        sampleFile.getAbsolutePath(),
                        Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        JSONAssert.assertEquals(
            new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)),
            new JSONArray((List<?>) outputs.get("readJSONFile")),
            true);
    }

    @Test
    public void testWrite() throws JSONException {
        Job job = componentWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("jsonfile_v1_write".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "source",
                new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)).toList()));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertThat(((Map) outputs.get("writeJSONFile")).get(WorkflowConstants.NAME))
            .isEqualTo("file.json");

        File sampleFile = getFile("sample_array.json");

        job = componentWorkflowTestSupport.execute(
            Base64.getEncoder()
                .encodeToString("jsonfile_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        sampleFile.getName(), Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        outputs = job.getOutputs();

        JSONAssert.assertEquals(
            new JSONArray(Files.contentOf(sampleFile, StandardCharsets.UTF_8)),
            new JSONArray((List<?>) outputs.get("readJSONFile")),
            true);
    }

    private File getFile(String filename) {
        return new File(JsonFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
