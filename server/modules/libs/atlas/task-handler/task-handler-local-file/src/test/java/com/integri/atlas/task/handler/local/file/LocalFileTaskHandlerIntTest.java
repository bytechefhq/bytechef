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

package com.integri.atlas.task.handler.local.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.test.task.handler.BaseTaskIntTest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class LocalFileTaskHandlerIntTest extends BaseTaskIntTest {

    @Test
    public void testRead() throws IOException {
        File sampleFile = getFile();

        Job job = startJob("samples/localFile_READ.json", Map.of("fileName", sampleFile.getAbsolutePath()));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        FileEntry fileEntry = fileStorageService.storeFileContent(
            "sample.txt",
            Files.contentOf(getFile(), Charset.defaultCharset())
        );

        assertThat(outputs.get("readLocalFile", FileEntry.class))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt")
            .hasFieldOrPropertyWithValue("url", fileEntry.getUrl());
    }

    @Test
    public void testWrite() throws IOException {
        File sampleFile = getFile();
        File tempFile = Files.newTemporaryFile();

        Job job = startJob(
            "samples/localFile_WRITE.json",
            Map.of(
                "fileEntry",
                fileStorageService.storeFileContent(
                    sampleFile.getAbsolutePath(),
                    Files.contentOf(getFile(), Charset.defaultCharset())
                ),
                "fileName",
                tempFile.getAbsolutePath()
            )
        );

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        assertThat((Map<?, ?>) outputs.get("writeLocalFile")).hasFieldOrPropertyWithValue("bytes", 5);
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerResolverMap() {
        return Map.of("localFile", new LocalFileTaskHandler(fileStorageService));
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }
}
