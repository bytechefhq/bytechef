
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

package com.bytechef.component.xmlfile;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.sync.executor.WorkflowExecutor;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.component.utils.XmlUtils;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class XmlFileComponentHandlerIntTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    @Test
    public void testRead() throws IOException {
        File sampleFile = getFile("sample.xml");

        Job job = workflowExecutor.execute(
            "xmlfile_v1_read",
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        sampleFile.getAbsolutePath(),
                        Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        assertThat((List<?>) outputs.get("readXMLFile"))
            .isEqualTo(XmlUtils.read(Files.contentOf(getFile("sample.xml"), StandardCharsets.UTF_8), List.class));
    }

    @Test
    public void testWrite() throws IOException {
        Job job = workflowExecutor.execute(
            "xmlfile_v1_write",
            Map.of(
                "source",
                XmlUtils.read(Files.contentOf(getFile("sample.xml"), StandardCharsets.UTF_8), List.class)));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        assertThat(((Map) outputs.get("writeXMLFile")).get(WorkflowConstants.NAME))
            .isEqualTo("file.xml");

        File sampleFile = getFile("sample.xml");

        job = workflowExecutor.execute(
            "xmlfile_v1_read",
            Map.of(
                "fileEntry",
                fileStorageService
                    .storeFileContent(
                        sampleFile.getName(), Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        outputs = job.getOutputs();

        assertThat((List<?>) outputs.get("readXMLFile"))
            .isEqualTo(XmlUtils.read(Files.contentOf(sampleFile, StandardCharsets.UTF_8), List.class));
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
