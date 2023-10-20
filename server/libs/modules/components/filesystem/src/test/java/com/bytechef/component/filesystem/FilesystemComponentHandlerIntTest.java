
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

package com.bytechef.component.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.WorkflowFileStorage;
import com.bytechef.hermes.component.test.JobTestExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class FilesystemComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JobTestExecutor jobTestExecutor;

    @Autowired
    private WorkflowFileStorage workflowFileStorage;

    @Test
    public void testRead() {
        File sampleFile = getFile();

        Job job = jobTestExecutor.execute(
            ENCODER.encodeToString("filesystem_v1_readFile".getBytes(StandardCharsets.UTF_8)),
            Map.of("filename", sampleFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = workflowFileStorage.readJobOutputs(job.getOutputs());

        FileEntry fileEntry = fileStorageService.storeFileContent(
            "data", "sample.txt", Files.contentOf(getFile(), StandardCharsets.UTF_8));

        assertThat(outputs.get("readLocalFile"))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt")
            .hasFieldOrPropertyWithValue("url", fileEntry.getUrl());
    }

    @Test
    public void testWrite() throws IOException {
        File sampleFile = getFile();
        File tempFile = Files.newTemporaryFile();

        Job job = jobTestExecutor.execute(
            ENCODER
                .encodeToString("filesystem_v1_writeFile".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        "data",
                        sampleFile.getAbsolutePath(),
                        Files.contentOf(getFile(), StandardCharsets.UTF_8))
                    .toMap(),
                "filename",
                tempFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = workflowFileStorage.readJobOutputs(job.getOutputs());

        assertThat((Map<?, ?>) outputs.get("writeLocalFile")).hasFieldOrPropertyWithValue("bytes", 5);
    }

    private File getFile() {
        return new File(FilesystemComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/sample.txt")
            .getFile());
    }
}
