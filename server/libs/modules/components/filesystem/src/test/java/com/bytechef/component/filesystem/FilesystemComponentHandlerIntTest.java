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

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.test.workflow.WorkflowExecutor;
import com.bytechef.hermes.component.test.MockFileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class FilesystemComponentHandlerIntTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @Test
    public void testRead() throws IOException {
        File sampleFile = getFile();

        Job job = workflowExecutor.execute("filesystem_v1_readFile", Map.of("filename", sampleFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        com.bytechef.hermes.file.storage.domain.FileEntry fileEntry =
                fileStorageService.storeFileContent("sample.txt", Files.contentOf(getFile(), Charset.defaultCharset()));

        assertThat(new MockFileEntry(outputs, "readLocalFile"))
                .hasFieldOrPropertyWithValue("extension", "txt")
                .hasFieldOrPropertyWithValue("mimeType", "text/plain")
                .hasFieldOrPropertyWithValue("name", "sample.txt")
                .hasFieldOrPropertyWithValue("url", fileEntry.getUrl());
    }

    @Test
    public void testWrite() throws IOException {
        File sampleFile = getFile();
        File tempFile = Files.newTemporaryFile();

        Job job = workflowExecutor.execute(
                "filesystem_v1_writeFile",
                Map.of(
                        "fileEntry",
                        fileStorageService
                                .storeFileContent(
                                        sampleFile.getAbsolutePath(),
                                        Files.contentOf(getFile(), Charset.defaultCharset()))
                                .toMap(),
                        "filename",
                        tempFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        assertThat((Map<?, ?>) outputs.get("writeLocalFile")).hasFieldOrPropertyWithValue("bytes", 5);
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }
}
