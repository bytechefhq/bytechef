
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

package com.bytechef.component.odsfile;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.test.JobTestExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.bytechef.hermes.execution.constants.FileEntryConstants;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import static com.bytechef.component.odsfile.constant.OdsFileConstants.FILE_ENTRY;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class OdsFileComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JobTestExecutor jobTestExecutor;

    @Autowired
    private WorkflowFileStorageFacade workflowFileStorageFacade;

    @Test
    public void testRead() throws IOException, JSONException {
        File sampleFile = getFile("sample_header.ods");

        try (FileInputStream fileInputStream = new FileInputStream(sampleFile)) {
            Job job = jobTestExecutor.execute(
                ENCODER.encodeToString("odsfile_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                    FILE_ENTRY,
                    fileStorageService.storeFileContent(
                        FileEntryConstants.DOCUMENTS_DIR, sampleFile.getAbsolutePath(), fileInputStream)));

            Assertions.assertThat(job.getStatus())
                .isEqualTo(Job.Status.COMPLETED);

            Map<String, ?> outputs = workflowFileStorageFacade.readJobOutputs(job.getOutputs());

            JSONAssert.assertEquals(
                new JSONArray(Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) outputs.get("readOdsFile")),
                true);
        }
    }

    @Test
    public void testWrite() throws IOException, JSONException {
        Job job = jobTestExecutor.execute(
            ENCODER.encodeToString("odsfile_v1_write".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                "rows",
                new JSONArray(Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8)).toList()));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = workflowFileStorageFacade.readJobOutputs(job.getOutputs());

        Assertions.assertThat(((Map) outputs.get("writeOdsFile")).get(WorkflowConstants.NAME))
            .isEqualTo("file.ods");

        File sampleFile = getFile("sample_header.ods");

        try (FileInputStream fileInputStream = new FileInputStream(sampleFile)) {
            job = jobTestExecutor.execute(
                ENCODER.encodeToString("odsfile_v1_read".getBytes(StandardCharsets.UTF_8)),
                Map.of(
                    FILE_ENTRY,
                    fileStorageService.storeFileContent(
                        FileEntryConstants.DOCUMENTS_DIR, sampleFile.getName(), fileInputStream)));

            outputs = workflowFileStorageFacade.readJobOutputs(job.getOutputs());

            JSONAssert.assertEquals(
                new JSONArray(Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) outputs.get("readOdsFile")),
                true);
        }
    }

    private File getFile(String fileName) {
        return new File(OdsFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + fileName)
            .getFile());
    }
}
