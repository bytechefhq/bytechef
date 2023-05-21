
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
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

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    public void testExecuteWrite() {
        Map<String, Object> source = Map.of(
            "Flower",
            new LinkedHashMap<>(
                Map.of(
                    "id", "45",
                    "name", "Poppy",
                    "color", "RED",
                    "petals", "9",
                    "Florists",
                    Map.of(
                        "Florist",
                        List.of(
                            new LinkedHashMap<>(Map.of("name", "Joe")),
                            new LinkedHashMap<>(Map.of("name", "Mark")))))));

        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mockedStatic.when(() -> MapValueUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILENAME), Mockito.eq("file.xml")))
                .thenReturn("file.xml");
            mockedStatic.when(() -> MapValueUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(source);

            XmlFileWriteAction.executeWrite(context, Map.of());

            ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(
                ByteArrayInputStream.class);
            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

            ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

            Assertions.assertThat(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo(
                    """
                        <root><Flower><id>45</id><petals>9</petals><color>RED</color><Florists><Florist><name>Joe</name></Florist><Florist><name>Mark</name></Florist></Florists><name>Poppy</name></Flower></root>
                        """);
            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo(FILE_XML);
        }

        Mockito.reset(context);

        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mockedStatic.when(() -> MapValueUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILENAME), Mockito.eq("file.xml")))
                .thenReturn(TEST_XML);
            mockedStatic.when(() -> MapValueUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(source);

            XmlFileWriteAction.executeWrite(context, Map.of());

            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo(TEST_XML);
        }
    }

    @Test
    public void testExecuteWriteArray() {
        List<Map<String, Object>> source = List.of(
            new LinkedHashMap<>(Map.of("id", "45", "name", "Poppy")),
            new LinkedHashMap<>(Map.of("id", "50", "name", "Rose")));

        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mockedStatic.when(() -> MapValueUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILENAME), Mockito.eq("file.xml")))
                .thenReturn("file.xml");
            mockedStatic.when(() -> MapValueUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(source);

            XmlFileWriteAction.executeWrite(context, Map.of());

            ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
                .forClass(ByteArrayInputStream.class);
            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

            ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

            Assertions.assertThat(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("""
                    <root><item><name>Poppy</name><id>45</id></item><item><name>Rose</name><id>50</id></item></root>
                    """);

            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo(FILE_XML);
        }

        Mockito.reset(context);

        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mockedStatic.when(() -> MapValueUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILENAME), Mockito.eq("file.xml")))
                .thenReturn(TEST_XML);
            mockedStatic.when(() -> MapValueUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(source);

            XmlFileWriteAction.executeWrite(context, Map.of());

            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo(TEST_XML);
        }
    }
}
