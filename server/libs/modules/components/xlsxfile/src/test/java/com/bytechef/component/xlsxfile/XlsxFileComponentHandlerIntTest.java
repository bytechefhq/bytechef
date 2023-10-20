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

package com.bytechef.component.xlsxfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.sync.executor.WorkflowExecutor;
import com.bytechef.commons.collection.MapUtils;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.component.test.json.JsonArrayUtils;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class XlsxFileComponentHandlerIntTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @Test
    public void testRead() throws IOException, JSONException {
        File sampleFile = getFile("sample_header.xlsx");

        Job job = workflowExecutor.execute(
                "xlsxfile_v1_read",
                Map.of(
                        "fileEntry",
                        fileStorageService
                                .storeFileContent(sampleFile.getAbsolutePath(), new FileInputStream(sampleFile))
                                .toMap()));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        JSONAssert.assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) outputs.get("readXlsxFile")),
                true);
    }

    @Test
    public void testWrite() throws IOException, JSONException {
        Job job = workflowExecutor.execute(
                "xlsxfile_v1_write",
                Map.of(
                        "rows",
                        JsonArrayUtils.toList(Files.contentOf(getFile("sample.json"), Charset.defaultCharset()))));

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        assertThat(MapUtils.getMapKey(outputs, "writeXlsxFile", WorkflowConstants.NAME))
                .isEqualTo("file.xlsx");

        File sampleFile = getFile("sample_header.xlsx");

        job = workflowExecutor.execute(
                "xlsxfile_v1_read",
                Map.of(
                        "fileEntry",
                        fileStorageService
                                .storeFileContent(sampleFile.getName(), new FileInputStream(sampleFile))
                                .toMap()));

        outputs = job.getOutputs();

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) outputs.get("readXlsxFile")),
                true);
    }

    private File getFile(String fileName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + fileName);

        return classPathResource.getFile();
    }
}
