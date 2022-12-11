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

import static com.bytechef.component.xmlfile.constants.XmlFileConstants.FILENAME;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.FILE_ENTRY;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.IS_ARRAY;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.PAGE_SIZE;
import static com.bytechef.component.xmlfile.constants.XmlFileConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.utils.XmlUtils;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class XmlFileComponentHandlerTest {

    private static final Context context = Mockito.mock(Context.class);
    private static final XmlFileComponentHandler xmlFileComponentHandler = new XmlFileComponentHandler();

    public static final String TEST_XML = "test.xml";
    public static final String FILE_XML = "file.xml";
    public static final String SAMPLE_ARRAY_XML = "sample_array.xml";
    public static final String SAMPLE_XML = "sample.xml";

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/xmlfile_v1.json", new XmlFileComponentHandler().getDefinition());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformRead() throws IOException {
        File file = getFile(SAMPLE_XML);

        Mockito.when(context.readFileToString(Mockito.any(FileEntry.class)))
                .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.get(FILE_ENTRY, FileEntry.class)).thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(executionParameters.getBoolean(IS_ARRAY, true)).thenReturn(false);

        assertThat((Map<String, ?>) xmlFileComponentHandler.performRead(context, executionParameters))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));
    }

    @Test
    public void testPerformReadArray() throws FileNotFoundException {
        File file = getFile(SAMPLE_ARRAY_XML);

        Mockito.when(context.getFileStream(Mockito.any(FileEntry.class))).thenReturn(new FileInputStream(file));

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.get(FILE_ENTRY, FileEntry.class)).thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(executionParameters.getBoolean(IS_ARRAY, true)).thenReturn(true);
        Mockito.when(executionParameters.getInteger(PAGE_NUMBER)).thenReturn(null);
        Mockito.when(executionParameters.getInteger(PAGE_SIZE)).thenReturn(null);

        assertThat((List<?>) xmlFileComponentHandler.performRead(context, executionParameters))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));

        Mockito.when(context.getFileStream(Mockito.any(FileEntry.class))).thenReturn(new FileInputStream(file));

        executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.get(FILE_ENTRY, FileEntry.class)).thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(executionParameters.getBoolean(IS_ARRAY, true)).thenReturn(true);
        Mockito.when(executionParameters.getInteger(PAGE_NUMBER)).thenReturn(1);
        Mockito.when(executionParameters.getInteger(PAGE_SIZE)).thenReturn(2);

        assertThat(((List<?>) xmlFileComponentHandler.performRead(context, executionParameters)).size())
                .isEqualTo(2);
    }

    @Test
    public void testPerformWrite() {
        File file = getFile(SAMPLE_XML);

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getRequired(SOURCE))
                .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));

        xmlFileComponentHandler.performWrite(context, executionParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor =
                ArgumentCaptor.forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context).storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertThat(XmlUtils.read(inputStreamArgumentCaptor.getValue(), new TypeReference<Map<String, Object>>() {}))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));
        Assertions.assertThat(filenameArgumentCaptor.getValue()).isEqualTo(FILE_XML);

        Mockito.reset(context);

        executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getString(FILENAME)).thenReturn(TEST_XML);
        Mockito.when(executionParameters.getRequired(SOURCE))
                .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));

        xmlFileComponentHandler.performWrite(context, executionParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context).storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(TEST_XML);
    }

    @Test
    public void testPerformWriteArray() {
        File file = getFile(SAMPLE_ARRAY_XML);

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getRequired(SOURCE))
                .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));

        xmlFileComponentHandler.performWrite(context, executionParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor =
                ArgumentCaptor.forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context).storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertThat(XmlUtils.read(inputStreamArgumentCaptor.getValue(), List.class))
                .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(FILE_XML);

        Mockito.reset(context);

        executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.getRequired(SOURCE))
                .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));
        Mockito.when(executionParameters.getString(FILENAME)).thenReturn(TEST_XML);

        xmlFileComponentHandler.performWrite(context, executionParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context).storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(TEST_XML);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
                .getClassLoader()
                .getResource("dependencies/" + filename)
                .getFile());
    }
}
