
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
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.util.MapUtils;
import com.bytechef.hermes.component.util.XmlUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    private final ActionContext actionContext = Mockito.mock(ActionContext.class);

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformRead() throws IOException {
        String sampleXml = "sample.xml";
        File file = getFile(sampleXml);
        Map<String, ?> map = Map.of(
            "Flower",
            Map.of(
                "id", "45",
                "name", "Poppy",
                "color", "RED",
                "petals", "9",
                "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))));

        try (MockedStatic<XmlUtils> xmlUtilsMockedStatic = Mockito.mockStatic(XmlUtils.class);
            MockedStatic<MapUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mapValueUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapValueUtilsMockedStatic.when(() -> MapUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(false);
            xmlUtilsMockedStatic.when(() -> XmlUtils.read(Mockito.anyString()))
                .thenReturn(map);

            Mockito.when(actionContext.readFileToString(Mockito.any(Context.FileEntry.class)))
                .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

            Assertions.assertThat((Map<String, ?>) XmlFileReadAction.perform(Map.of(), actionContext))
                .isEqualTo(map);
        }
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformReadArray() throws FileNotFoundException {
        String sampleArrayXml = "sample_array.xml";
        File file = getFile(sampleArrayXml);
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

        try (MockedStatic<XmlUtils> xmlUtilsMockedStatic = Mockito.mockStatic(XmlUtils.class);
            MockedStatic<MapUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapValueUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapValueUtilsMockedStatic.when(() -> MapUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapValueUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(null);
            mapValueUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(null);
            xmlUtilsMockedStatic.when(() -> XmlUtils.stream(Mockito.any()))
                .thenReturn(list.stream());

            Mockito.when(actionContext.getFileStream(Mockito.any(Context.FileEntry.class)))
                .thenReturn(new FileInputStream(file));

            Assertions.assertThat((List<?>) XmlFileReadAction.perform(Map.of(), actionContext))
                .isEqualTo(list);

            mapValueUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapValueUtilsMockedStatic.when(() -> MapUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapValueUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(1);
            mapValueUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(2);
            xmlUtilsMockedStatic.when(() -> XmlUtils.stream(Mockito.any()))
                .thenReturn(list.stream());

            Mockito.when(actionContext.getFileStream(Mockito.any(Context.FileEntry.class)))
                .thenReturn(new FileInputStream(file));

            Assertions.assertThat(((List<?>) XmlFileReadAction.perform(Map.of(), actionContext))
                .size())
                .isEqualTo(2);
        }
    }

    private File getFile(String filename) {
        return new File(XmlFileComponentHandlerIntTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
