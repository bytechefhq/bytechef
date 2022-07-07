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

package com.bytechef.task.handler.xlsxfile.v1_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.atlas.Accessor;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.task.commons.file.storage.FileStorageHelper;
import com.bytechef.test.json.JsonArrayUtils;
import com.bytechef.test.task.BaseTaskIntTest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class XLSXFileTaskHandlerIntTest extends BaseTaskIntTest {

    @Autowired
    private FileStorageHelper fileStorageHelper;

    @Test
    public void testRead() throws IOException {
        File sampleFile = getFile("sample_header.xlsx");

        Job job = startJob(
                "samples/v1_0/xlsxFile_READ.json",
                Map.of(
                        "fileEntry",
                        fileStorageHelper.storeFileContent(
                                sampleFile.getAbsolutePath(), new FileInputStream(sampleFile))));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        JSONAssert.assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) outputs.get("readXlsxFile")),
                true);
    }

    @Test
    public void testWrite() throws IOException {
        Job job = startJob(
                "samples/v1_0/xlsxFile_WRITE.json",
                Map.of(
                        "rows",
                        JsonArrayUtils.toList(Files.contentOf(getFile("sample.json"), Charset.defaultCharset()))));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Accessor outputs = job.getOutputs();

        FileEntry fileEntry = outputs.get("writeXlsxFile", FileEntry.class);
        File sampleFile = getFile("sample_header.xlsx");

        job = startJob(
                "samples/v1_0/xlsxFile_READ.json",
                Map.of(
                        "fileEntry",
                        fileStorageHelper.storeFileContent(sampleFile.getName(), new FileInputStream(sampleFile))));

        outputs = job.getOutputs();

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) outputs.get("readXlsxFile")),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.xlsx");
    }

    private File getFile(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + fileName);

        return classPathResource.getFile();
    }
}
