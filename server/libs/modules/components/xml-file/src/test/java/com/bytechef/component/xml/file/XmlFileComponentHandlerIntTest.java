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

package com.bytechef.component.xml.file;

import static com.bytechef.component.xml.file.constant.XmlFileConstants.FILE_ENTRY;
import static com.bytechef.component.xml.file.constant.XmlFileConstants.SOURCE;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.test.ComponentJobTestExecutor;
import com.bytechef.platform.component.test.annotation.ComponentIntTest;
import com.bytechef.platform.workflow.execution.constants.FileEntryConstants;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
@ComponentIntTest
public class XmlFileComponentHandlerIntTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Test
    public void testRead() {
        File sampleFile = getFile("sample.xml");

        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("xml-file_v1_read".getBytes(StandardCharsets.UTF_8)),
            Map.of(
                FILE_ENTRY,
                fileStorageService.storeFileContent(
                    FileEntryConstants.FILES_DIR, sampleFile.getAbsolutePath(),
                    Files.contentOf(sampleFile, StandardCharsets.UTF_8))));

        Assertions.assertThat(job.getStatus())
            .isEqualTo(Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        Assertions.assertThat(Map.of(
            "Flower",
            Map.of(
                "id", "45",
                "name", "Poppy",
                "color", "RED",
                "petals", "9",
                "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark"))))))
            .isEqualTo(outputs.get("readXMLFile"));
    }

    @Test
    public void testWrite() {
        Job job = componentJobTestExecutor.execute(
            ENCODER.encodeToString("xml-file_v1_write".getBytes(StandardCharsets.UTF_8)),
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

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        FileEntry fileEntry = MapUtils.get(outputs, "writeXMLFile", FileEntry.class);

        Assertions.assertThat(fileEntry.getName())
            .isEqualTo("file.xml");

        Assertions.assertThat(fileStorageService.readFileToString(FileEntryConstants.FILES_DIR, fileEntry))
            .isEqualTo(
                """
                    <root><Flower><color>RED</color><Florists><Florist><name>Joe</name></Florist><Florist><name>Mark</name></Florist></Florists><name>Poppy</name><id>45</id><petals>9</petals></Flower></root>
                    """);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/xml-file/" + filename)
            .getFile());
    }
}
