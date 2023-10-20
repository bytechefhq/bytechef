
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
import com.bytechef.hermes.component.util.MapValueUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    private static final Context context = Mockito.mock(Context.class);

    private static final String SAMPLE_ARRAY_XML = "sample_array.xml";
    private static final String SAMPLE_XML = "sample.xml";

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteRead() throws IOException {
        File file = getFile(SAMPLE_XML);

        try (MockedStatic<MapValueUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(false);

            Mockito.when(context.readFileToString(Mockito.any(Context.FileEntry.class)))
                .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

            Assertions.assertThat((Map<String, ?>) XmlFileReadAction.executeRead(context, Map.of()))
                .isEqualTo(
                    Map.of(
                        "Flower",
                        Map.of(
                            "id", "45",
                            "name", "Poppy",
                            "color", "RED",
                            "petals", "9",
                            "Florists", Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark"))))));
        }
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testExecuteReadArray() throws FileNotFoundException {
        File file = getFile(SAMPLE_ARRAY_XML);

        try (MockedStatic<MapValueUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(null);
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(null);

            Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
                .thenReturn(new FileInputStream(file));

            Assertions.assertThat((List<?>) XmlFileReadAction.executeRead(context, Map.of()))
                .isEqualTo(
                    List.of(
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
                            "petals", "5")));

            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(1);
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(2);

            Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
                .thenReturn(new FileInputStream(file));

            Assertions.assertThat(((List<?>) XmlFileReadAction.executeRead(context, Map.of()))
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
