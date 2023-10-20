
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

package com.bytechef.component.jsonfile.action;

import com.bytechef.component.jsonfile.JsonFileComponentHandlerTest;
import com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FileType;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.hermes.component.util.MapUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILENAME;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.SOURCE;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author Ivica Cardic
 */
public class JsonFileWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    public void testPerformWriteJSON() throws JSONException {
        File file = getFile("sample.json");

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapUtilsMockedStatic.when(() -> MapUtils.getString(Mockito.anyMap(), Mockito.eq(FILENAME)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSON");
            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)).toMap());
            jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.any()))
                .thenReturn(
                    Files.contentOf(file, StandardCharsets.UTF_8));

            JsonFileWriteAction.perform(Map.of(), context);

            ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(
                ByteArrayInputStream.class);
            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

            ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

            assertEquals(
                new JSONObject(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8)),
                new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)),
                true);
            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo("file.json");
        }
    }

    @Test
    public void testPerformWriteJSONArray() throws JSONException {
        File file = getFile("sample_array.json");

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapUtilsMockedStatic.when(() -> MapUtils.getString(Mockito.anyMap(), Mockito.eq(FILENAME)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSON");
            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)).toList());
            jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.any()))
                .thenReturn(
                    Files.contentOf(file, StandardCharsets.UTF_8));

            JsonFileWriteAction.perform(Map.of(), context);

            ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
                .forClass(ByteArrayInputStream.class);
            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

            assertEquals(
                new JSONArray(new String(inputStreamArgumentCaptor.getValue()
                    .readAllBytes(), StandardCharsets.UTF_8)),
                new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)),
                true);
            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo("file.json");
        }

        Mockito.reset(context);

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mapUtilsMockedStatic.when(() -> MapUtils.getString(Mockito.anyMap(), Mockito.eq(FILENAME)))
                .thenReturn("test.json");
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSON");
            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)).toList());
            jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.any()))
                .thenReturn(
                    Files.contentOf(file, StandardCharsets.UTF_8));

            JsonFileWriteAction.perform(Map.of(), context);

            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo("test.json");
        }
    }

    @Test
    public void testPerformWriteJSONL() throws JSONException {
        File file = getFile("sample.jsonl");

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mapUtilsMockedStatic.when(() -> MapUtils.getString(Mockito.anyMap(), Mockito.eq(FILENAME)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSONL");
            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList());
            jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.any()))
                .thenReturn(
                    Files.contentOf(file, StandardCharsets.UTF_8));

            for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
                jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.eq(new JSONObject(line).toMap())))
                    .thenReturn(line);
            }

            JsonFileWriteAction.perform(
                Map.of(FILE_TYPE, "JSONL", SOURCE, linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList()),
                context);

            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
                .forClass(ByteArrayInputStream.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

            ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

            assertEquals(
                linesOf(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8)),
                linesOf(Files.contentOf(file, StandardCharsets.UTF_8)),
                true);
            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo("file.jsonl");
        }

        Mockito.reset(context);

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mapUtilsMockedStatic.when(() -> MapUtils.getString(Mockito.anyMap(), Mockito.eq(FILENAME)))
                .thenReturn("test.jsonl");
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSONL");
            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(Mockito.anyMap(), Mockito.eq(SOURCE)))
                .thenReturn(linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList());

            for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
                jsonUtilsMockedStatic.when(() -> JsonUtils.write(Mockito.eq(new JSONObject(line).toMap())))
                    .thenReturn(line);
            }

            JsonFileWriteAction.perform(
                Map.of(
                    FILENAME, "test.jsonl",
                    FILE_TYPE, "JSONL",
                    SOURCE, linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList()),
                context);

            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

            Assertions.assertThat(filenameArgumentCaptor.getValue())
                .isEqualTo("test.jsonl");
        }
    }

    private File getFile(String filename) {
        return new File(JsonFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }

    private static JSONArray linesOf(String jsonl) {
        return new JSONArray("[" + jsonl.replace("\n", ",") + "]");
    }
}
