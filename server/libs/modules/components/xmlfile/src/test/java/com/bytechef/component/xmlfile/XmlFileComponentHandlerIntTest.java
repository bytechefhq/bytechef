
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

import com.bytechef.atlas.domain.Job;
import com.bytechef.hermes.component.test.workflow.ComponentWorkflowTestSupport;
import com.bytechef.hermes.component.test.annotation.ComponentIntTest;
import com.bytechef.hermes.file.storage.domain.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILE_ENTRY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.SOURCE;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class XmlFileComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ComponentWorkflowTestSupport componentWorkflowTestSupport;

    @Test
    public void testRead() {
        File sampleFile = getFile("sample.xml");

        Job job = componentWorkflowTestSupport.execute(
            ENCODER.encodeToString("xmlfile_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                FILE_ENTRY,
                fileStorageService.storeFileContent(
                    sampleFile.getAbsolutePath(), Files.contentOf(sampleFile, StandardCharsets.UTF_8))
                    .toMap()));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Assertions.assertThat(Map.of(
            "Flower",
            Map.of(
                "id", "45",
                "name", "Poppy",
                "color", "RED",
                "petals", "9",
                "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark"))))))
            .isEqualTo((Map<?, ?>) outputs.get("readXMLFile"));
    }

    @Test
    public void testWrite() {
        Job job = componentWorkflowTestSupport.execute(
            ENCODER.encodeToString("xmlfile_v1_write".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                SOURCE,
                Map.of(
                    "Flower",
                    Map.of(
                        "id", "45",
                        "name", "Poppy",
                        "color", "RED",
                        "petals", "9",
                        "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))))));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, Object> outputs = job.getOutputs();

        Map<?, ?> fileEntryMap = (Map<?, ?>) outputs.get("writeXMLFile");

        Assertions.assertThat(fileEntryMap.get("name"))
            .isEqualTo("file.xml");

        Assertions.assertThat(
            fileStorageService.readFileToString(
                new FileEntry(
                    (String) fileEntryMap.get("name"), (String) fileEntryMap.get("extension"),
                    (String) fileEntryMap.get("mimeType"), (String) fileEntryMap.get("url"))))
            .isEqualTo(
                """
                    <root><Flower><color>RED</color><Florists><Florist><name>Joe</name></Florist><Florist><name>Mark</name></Florist></Florists><name>Poppy</name><id>45</id><petals>9</petals></Flower></root>
                    """);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
