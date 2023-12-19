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

package com.bytechef.component.json.file;

import static com.bytechef.component.json.file.constant.JsonFileConstants.SOURCE;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.test.ComponentJobTestExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.execution.constants.FileEntryConstants;
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

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class JsonFileComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Test
    public void testRead() throws JSONException {
        File sampleFile = getFile("sample_array.json");

        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("json-file_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        FileEntryConstants.FILES_DIR, sampleFile.getAbsolutePath(),
                        Files.contentOf(sampleFile, StandardCharsets.UTF_8))));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        JSONAssert.assertEquals(
            new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)),
            new JSONArray((List<?>) outputs.get("readJSONFile")),
            true);
    }

    @Test
    public void testWrite() throws JSONException {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("json-file_v1_write".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                SOURCE,
                new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)).toList()));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        FileEntry fileEntry = MapUtils.get(
            taskFileStorage.readJobOutputs(job.getOutputs()), "writeJSONFile", FileEntry.class);

        Assertions.assertThat(fileEntry.getName())
            .isEqualTo("file.json");

        JSONAssert.assertEquals(
            new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)),
            new JSONArray(fileStorageService.readFileToString(FileEntryConstants.FILES_DIR, fileEntry)),
            true);
    }

    private File getFile(String filename) {
        return new File(JsonFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/json-file/" + filename)
            .getFile());
    }
}
