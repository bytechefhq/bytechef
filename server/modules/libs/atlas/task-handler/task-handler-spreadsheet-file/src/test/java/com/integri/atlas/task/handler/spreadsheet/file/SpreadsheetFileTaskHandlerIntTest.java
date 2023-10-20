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

package com.integri.atlas.task.handler.spreadsheet.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.json.JSONArrayUtil;
import com.integri.atlas.task.handler.BaseTaskIntTest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class SpreadsheetFileTaskHandlerIntTest extends BaseTaskIntTest {

    @Test
    public void testRead() throws IOException {
        File sampleFile = getFile("sample_header.csv");

        Job job = startJob(
            "samples/spreadsheetFile_READ.json",
            Map.of(
                "fileEntry",
                fileStorageService.storeFile(
                    sampleFile.getAbsolutePath(),
                    Files.contentOf(sampleFile, Charset.defaultCharset())
                )
            )
        );

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        JSONAssert.assertEquals(
            JSONArrayUtil.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
            JSONArrayUtil.of((List<?>) outputs.get("readFromSpreadsheet")),
            true
        );
    }

    @Test
    public void testWrite() throws IOException {
        Job job = startJob(
            "samples/spreadsheetFile_WRITE.json",
            Map.of("items", JSONArrayUtil.toList(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())))
        );

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        FileEntry fileEntry = outputs.get("writeToSpreadsheet", FileEntry.class);
        File sampleFile = getFile("sample_header.csv");

        job =
            startJob(
                "samples/spreadsheetFile_READ.json",
                Map.of(
                    "fileEntry",
                    fileStorageService.storeFile(
                        sampleFile.getName(),
                        Files.contentOf(sampleFile, Charset.defaultCharset())
                    )
                )
            );

        outputs = job.getOutputs();

        assertEquals(
            JSONArrayUtil.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
            JSONArrayUtil.of((List<?>) outputs.get("readFromSpreadsheet")),
            true
        );

        assertThat(fileEntry.getName()).isEqualTo("spreadsheet.csv");
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerResolverMap() {
        return Map.of("spreadsheetFile", new SpreadsheetFileTaskHandler(fileStorageService));
    }

    private File getFile(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + fileName);

        return classPathResource.getFile();
    }
}
