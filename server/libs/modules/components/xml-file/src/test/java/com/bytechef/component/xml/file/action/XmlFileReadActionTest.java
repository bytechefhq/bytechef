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

package com.bytechef.component.xml.file.action;

import com.bytechef.component.xml.file.XmlFileComponentHandlerIntTest;
import com.bytechef.component.xml.file.constant.XmlFileConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class XmlFileReadActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformRead() throws IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample.xml");
        Map<String, ?> map = Map.of(
            "Flower",
            Map.of(
                "id", "45",
                "name", "Poppy",
                "color", "RED",
                "petals", "9",
                "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))));
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(XmlFileConstants.FILE_ENTRY)))
            .thenReturn(Mockito.mock(ActionContext.FileEntry.class));
        Mockito.when(parameters.getBoolean(Mockito.eq(XmlFileConstants.IS_ARRAY), Mockito.eq(true)))
            .thenReturn(false);
        Mockito.when(context.xml(Mockito.any()))
            .thenReturn(map);

        Mockito.when(context.file(file1 -> file1.readToString(Mockito.any(ActionContext.FileEntry.class))))
            .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

        Assertions.assertThat((Map<String, ?>) XmlFileReadAction.perform(parameters, parameters, context))
            .isEqualTo(map);
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformReadArray() throws FileNotFoundException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample_array.xml");
        List<?> list = List.of(
            Map.of(
                "id", "45",
                "name", "Poppy",
                "color", "RED",
                "petals", "9",
                "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))),
            Map.of(
                "id", "46",
                "name", "Rose",
                "color", "YELLOW",
                "petals", "5"));
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequired(
            Mockito.eq(XmlFileConstants.FILE_ENTRY), Mockito.eq(ActionContext.FileEntry.class)))
            .thenReturn(Mockito.mock(ActionContext.FileEntry.class));
        Mockito.when(parameters.getBoolean(
            Mockito.eq(XmlFileConstants.IS_ARRAY), Mockito.eq(true)))
            .thenReturn(true);
        Mockito.when(parameters.getInteger(Mockito.eq(XmlFileConstants.PAGE_NUMBER)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(XmlFileConstants.PAGE_SIZE)))
            .thenReturn(null);
        Mockito.when(context.xml(Mockito.any()))
            .thenReturn(list.stream());

        Mockito.when(context.file(file1 -> file1.getStream(Mockito.any(ActionContext.FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Assertions.assertThat((List<?>) XmlFileReadAction.perform(parameters, parameters, context))
            .isEqualTo(list);

        Mockito.when(parameters.getRequired(
            Mockito.eq(XmlFileConstants.FILE_ENTRY), Mockito.eq(ActionContext.FileEntry.class)))
            .thenReturn(Mockito.mock(ActionContext.FileEntry.class));
        Mockito.when(parameters.getBoolean(
            Mockito.eq(XmlFileConstants.IS_ARRAY), Mockito.eq(true)))
            .thenReturn(true);
        Mockito.when(parameters.getInteger(Mockito.eq(XmlFileConstants.PAGE_NUMBER)))
            .thenReturn(1);
        Mockito.when(parameters.getInteger(Mockito.eq(XmlFileConstants.PAGE_SIZE)))
            .thenReturn(2);
        Mockito.when(context.xml(Mockito.any()))
            .thenReturn(list.stream());

        Mockito.when(context.file(file1 -> file1.getStream(Mockito.any(ActionContext.FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Assertions.assertThat(((List<?>) XmlFileReadAction.perform(parameters, parameters, context))
            .size())
            .isEqualTo(2);
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/xml-file/" + filename)
            .getFile());
    }
}
