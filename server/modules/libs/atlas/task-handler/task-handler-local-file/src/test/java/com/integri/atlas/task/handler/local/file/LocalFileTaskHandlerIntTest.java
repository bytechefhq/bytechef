/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.integri.atlas.engine.core.binary.Binary;
import com.integri.atlas.task.handler.BaseTaskHandlerIntTest;
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
public class LocalFileTaskHandlerIntTest extends BaseTaskHandlerIntTest {

    @Test
    public void testRead() throws IOException {
        File sampleFile = getFile();

        Job job = startJob(
            "samples/localFile_READ.json",
            Map.of("localFile", new LocalFileTaskHandler(binaryHelper)),
            Map.of("fileName", sampleFile.getAbsolutePath())
        );

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        assertThat(outputs.get("readFromFile", Binary.class))
            .hasFieldOrPropertyWithValue(
                "data",
                storageService.write("bucketName", Files.contentOf(getFile(), Charset.defaultCharset()))
            )
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt");
    }

    @Test
    public void testWrite() throws IOException {
        File sampleFile = getFile();
        File tempFile = Files.newTemporaryFile();

        Job job = startJob(
            "samples/localFile_WRITE.json",
            Map.of("localFile", new LocalFileTaskHandler(binaryHelper)),
            Map.of(
                "binary",
                binaryHelper.writeBinaryData(
                    sampleFile.getAbsolutePath(),
                    Files.contentOf(getFile(), Charset.defaultCharset())
                ),
                "fileName",
                tempFile.getAbsolutePath()
            )
        );

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        assertThat((Map<?, ?>) outputs.get("writeToFile")).hasFieldOrPropertyWithValue("bytes", 5);
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }
}
