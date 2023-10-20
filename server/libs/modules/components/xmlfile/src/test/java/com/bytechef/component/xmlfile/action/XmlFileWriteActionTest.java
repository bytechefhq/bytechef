
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

package com.bytechef.component.xmlfile.action;

import com.bytechef.component.xmlfile.XmlFileComponentHandlerIntTest;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.util.XmlUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILENAME;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.SOURCE;

/**
 * @author Ivica Cardic
 */
public class XmlFileWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    private static final String TEST_XML = "test.xml";
    private static final String FILE_XML = "file.xml";
    private static final String SAMPLE_ARRAY_XML = "sample_array.xml";
    private static final String SAMPLE_XML = "sample.xml";

    @BeforeAll
    public static void beforeAll() {
//        ReflectionTestUtils.setField(XmlUtils.class, "xmlMapper", new XmlMapper());
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    public void testExecuteWrite() {
        File file = getFile(SAMPLE_XML);

        Map<String, ?> inputParameters = Map.of(
            SOURCE, XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8)));

        XmlFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        Assertions.assertThat((Map<?, ?>) XmlUtils.read(inputStreamArgumentCaptor.getValue()))
            .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8)));
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo(FILE_XML);

        Mockito.reset(context);

        inputParameters = Map.of(
            FILENAME, TEST_XML,
            SOURCE, XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8)));

        XmlFileWriteAction.executeWrite(context, inputParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo(TEST_XML);
    }

    @Test
    public void testExecuteWriteArray() {
        File file = getFile(SAMPLE_ARRAY_XML);

        Map<String, ?> inputParameters = Map.of(
            SOURCE, XmlUtils.readList(Files.contentOf(file, StandardCharsets.UTF_8)));

        XmlFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        Assertions.assertThat(XmlUtils.readList(inputStreamArgumentCaptor.getValue()))
            .isEqualTo(XmlUtils.readList(Files.contentOf(file, StandardCharsets.UTF_8)));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo(FILE_XML);

        Mockito.reset(context);

        inputParameters = Map.of(
            SOURCE, XmlUtils.readList(Files.contentOf(file, StandardCharsets.UTF_8)),
            FILENAME, TEST_XML);

        XmlFileWriteAction.executeWrite(context, inputParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo(TEST_XML);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
