
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.util.XmlMapper;
import com.bytechef.hermes.component.util.XmlUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.xmlfile.constant.XmlFileConstants.FILE_ENTRY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.IS_ARRAY;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xmlfile.constant.XmlFileConstants.PAGE_SIZE;

/**
 * @author Ivica Cardic
 */
public class XmlFileReadActionTest {

    private static final Context context = Mockito.mock(Context.class);

    private static final String SAMPLE_ARRAY_XML = "sample_array.xml";
    private static final String SAMPLE_XML = "sample.xml";

    @BeforeAll
    public static void beforeAll() {
        ReflectionTestUtils.setField(XmlUtils.class, "xmlMapper", new XmlMapper());
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
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

        Assertions.assertThat((Map<String, ?>) XmlFileReadAction.executeRead(context, inputParameters))
            .isEqualTo(XmlUtils.read(Files.contentOf(file, StandardCharsets.UTF_8)));
    }

    @Test
    @SuppressFBWarnings("OBL")
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

        Assertions.assertThat((List<?>) XmlFileReadAction.executeRead(context, inputParameters))
            .isEqualTo(XmlUtils.readList(Files.contentOf(file, StandardCharsets.UTF_8)));

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

        Assertions.assertThat(((List<?>) XmlFileReadAction.executeRead(context, inputParameters)).size())
            .isEqualTo(2);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
