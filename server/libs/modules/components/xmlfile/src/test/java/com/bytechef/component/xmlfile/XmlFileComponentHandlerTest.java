
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

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILENAME;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILE_ENTRY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.IS_ARRAY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PAGE_SIZE;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.SOURCE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.xmlfile.action.XmlFileReadAction;
import com.bytechef.component.xmlfile.action.XmlFileWriteAction;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.util.XmlUtils;
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
    public void testExecuteRead() throws IOException {
        File file = getFile(SAMPLE_XML);

        Mockito.when(context.readFileToString(Mockito.any(Context.FileEntry.class)))
            .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.get(FILE_ENTRY, Context.FileEntry.class))
            .thenReturn(Mockito.mock(Context.FileEntry.class));
        Mockito.when(inputParameters.getBoolean(IS_ARRAY, true))
            .thenReturn(false);

        assertThat((Map<String, ?>) XmlFileReadAction.executeRead(context, inputParameters))
            .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));
    }

    @Test
    public void testExecuteReadArray() throws FileNotFoundException {
        File file = getFile(SAMPLE_ARRAY_XML);

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.get(FILE_ENTRY, Context.FileEntry.class))
            .thenReturn(Mockito.mock(Context.FileEntry.class));
        Mockito.when(inputParameters.getBoolean(IS_ARRAY, true))
            .thenReturn(true);
        Mockito.when(inputParameters.getInteger(PAGE_NUMBER))
            .thenReturn(null);
        Mockito.when(inputParameters.getInteger(PAGE_SIZE))
            .thenReturn(null);

        assertThat((List<?>) XmlFileReadAction.executeRead(context, inputParameters))
            .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.get(FILE_ENTRY, Context.FileEntry.class))
            .thenReturn(Mockito.mock(Context.FileEntry.class));
        Mockito.when(inputParameters.getBoolean(IS_ARRAY, true))
            .thenReturn(true);
        Mockito.when(inputParameters.getInteger(PAGE_NUMBER))
            .thenReturn(1);
        Mockito.when(inputParameters.getInteger(PAGE_SIZE))
            .thenReturn(2);

        assertThat(((List<?>) XmlFileReadAction.executeRead(context, inputParameters)).size())
            .isEqualTo(2);
    }

    @Test
    public void testExecuteWrite() {
        File file = getFile(SAMPLE_XML);

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));

        XmlFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertThat(XmlUtils.read(inputStreamArgumentCaptor.getValue(), new TypeReference<Map<String, Object>>() {}))
            .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo(FILE_XML);

        Mockito.reset(context);

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILENAME))
            .thenReturn(TEST_XML);
        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), Map.class));

        XmlFileWriteAction.executeWrite(context, inputParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(TEST_XML);
    }

    @Test
    public void testExecuteWriteArray() {
        File file = getFile(SAMPLE_ARRAY_XML);

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));

        XmlFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertThat(XmlUtils.read(inputStreamArgumentCaptor.getValue(), List.class))
            .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(FILE_XML);

        Mockito.reset(context);

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8), List.class));
        Mockito.when(inputParameters.getString(FILENAME))
            .thenReturn(TEST_XML);

        XmlFileWriteAction.executeWrite(context, inputParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(TEST_XML);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
