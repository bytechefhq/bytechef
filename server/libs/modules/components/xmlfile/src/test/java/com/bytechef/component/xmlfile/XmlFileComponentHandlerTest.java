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

import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static com.bytechef.hermes.component.definition.ActionDefinition.ACTION;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.xmlfile.constants.XmlFileConstants;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.hermes.component.utils.XmlUtils;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class XmlFileComponentHandlerTest {

    private static final MockContext context = new MockContext();
    private static final XmlFileComponentHandler xmlFileComponentHandler = new XmlFileComponentHandler();

    public static final String TEST_XML = "test.xml";
    public static final String FILE_XML = "file.xml";
    public static final String SAMPLE_ARRAY_XML = "sample_array.xml";
    public static final String SAMPLE_XML = "sample.xml";

    @Test
    public void testGetComponentDefinition() {
        AssertUtils.assertEquals("definition/xmlfile_v1.json", new XmlFileComponentHandler().getDefinition());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformRead() throws IOException {
        File file = getFile(SAMPLE_XML);

        MockExecutionParameters parameters = new MockExecutionParameters();

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            parameters.set(
                    FILE_ENTRY,
                    context.storeFileContent(file.getName(), fileInputStream).toMap());
            parameters.set(XmlFileConstants.IS_ARRAY, false);
        }

        assertThat((Map<String, ?>) xmlFileComponentHandler.performRead(context, parameters))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), Map.class));
    }

    @Test
    public void testPerformReadArray() throws IOException {
        File file = getFile(SAMPLE_ARRAY_XML);

        MockExecutionParameters parameters = new MockExecutionParameters();

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            parameters.set(
                    FILE_ENTRY,
                    context.storeFileContent(file.getName(), fileInputStream).toMap());
        }

        assertThat((List<?>) xmlFileComponentHandler.performRead(context, parameters))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());
        parameters.set(XmlFileConstants.PAGE_NUMBER, 1);
        parameters.set(XmlFileConstants.PAGE_SIZE, 2);

        assertThat(((List<?>) xmlFileComponentHandler.performRead(context, parameters)).size())
                .isEqualTo(2);
    }

    @Test
    public void testPerformWrite() throws IOException {
        File file = getFile(SAMPLE_XML);

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                XmlFileConstants.SOURCE, XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), Map.class));
        parameters.set(ACTION, XmlFileConstants.WRITE);

        FileEntry fileEntry = (FileEntry) xmlFileComponentHandler.performWrite(context, parameters);

        assertThat(XmlUtils.read(context.readFileToString(fileEntry), List.class))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        assertThat(fileEntry.getName()).isEqualTo(FILE_XML);

        parameters.set(FILENAME, TEST_XML);
        parameters.set(
                XmlFileConstants.SOURCE, XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), Map.class));

        fileEntry = (FileEntry) xmlFileComponentHandler.performWrite(context, parameters);

        assertThat(fileEntry.getName()).isEqualTo(TEST_XML);
    }

    @Test
    public void testPerformWriteArray() throws IOException {
        File file = getFile(SAMPLE_ARRAY_XML);

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                XmlFileConstants.SOURCE, XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        FileEntry fileEntry = (FileEntry) xmlFileComponentHandler.performWrite(context, parameters);

        assertThat(XmlUtils.read(context.readFileToString(fileEntry), List.class))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), List.class));

        assertThat(fileEntry.getName()).isEqualTo(FILE_XML);

        parameters.set(FILENAME, TEST_XML);
        parameters.set(
                XmlFileConstants.SOURCE, XmlUtils.read(Files.contentOf(file, Charset.defaultCharset()), List.class));
        parameters.set(ACTION, XmlFileConstants.WRITE);

        fileEntry = (FileEntry) xmlFileComponentHandler.performWrite(context, parameters);

        assertThat(fileEntry.getName()).isEqualTo(TEST_XML);
    }

    private File getFile(String filename) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + filename);

        return classPathResource.getFile();
    }
}
