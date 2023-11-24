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

package com.bytechef.component.filesystem;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.test.ComponentJobTestExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.execution.constants.FileEntryConstants;
import java.io.File;
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
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Test
    public void testRead() {
        File sampleFile = getFile();

        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("filesystem_v1_readFile".getBytes(StandardCharsets.UTF_8)),
            Map.of("filename", sampleFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        FileEntry fileEntry = fileStorageService.storeFileContent(
            FileEntryConstants.FILES_DIR, "sample.txt", Files.contentOf(getFile(), StandardCharsets.UTF_8));

        assertThat(outputs.get("readLocalFile"))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt")
            .hasFieldOrPropertyWithValue("url", fileEntry.getUrl());
    }

    @Test
    public void testWrite() {
        File sampleFile = getFile();
        File tempFile = Files.newTemporaryFile();

        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("filesystem_v1_writeFile".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                FILE_ENTRY,
                fileStorageService
                    .storeFileContent(
                        FileEntryConstants.FILES_DIR, sampleFile.getAbsolutePath(),
                        Files.contentOf(getFile(), StandardCharsets.UTF_8)),
                "filename", tempFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertThat((Map<?, ?>) outputs.get("writeLocalFile")).hasFieldOrPropertyWithValue("bytes", 5);
    }

    private File getFile() {
        return new File(FilesystemComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/sample.txt")
            .getFile());
    }
}
