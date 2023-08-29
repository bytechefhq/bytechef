
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_ENTRY;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.IS_ARRAY;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.PAGE_NUMBER;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.PAGE_SIZE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.PATH;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author Ivica Cardic
 */
public class JsonFileReadActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformReadJSON() throws JSONException, IOException {
        File file = getFile("sample.json");

        Mockito.when(context.readFileToString(Mockito.any(Context.FileEntry.class)))
            .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSON");
            mapUtilsMockedStatic
                .when(() -> MapUtils.getBoolean(Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(false)))
                .thenReturn(true);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(null);
            jsonUtilsMockedStatic.when(() -> JsonUtils.read(Mockito.anyString()))
                .thenReturn(new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)).toMap());

            assertEquals(
                new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)),
                new JSONObject((Map<String, ?>) JsonFileReadAction.perform(Map.of(), context)),
                true);
        }
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformReadJSONArray() throws JSONException, FileNotFoundException {
        File file = getFile("sample_array.json");

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSON");
            mapUtilsMockedStatic.when(
                () -> MapUtils.getBoolean(Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapUtilsMockedStatic.when(() -> MapUtils.getString(Mockito.anyMap(), Mockito.eq(PATH)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(null);
            jsonUtilsMockedStatic.when(() -> JsonUtils.stream(Mockito.any()))
                .thenReturn(
                    new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8))
                        .toList()
                        .stream());

            assertEquals(
                new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)),
                new JSONArray((List<?>) JsonFileReadAction.perform(Map.of(), context)),
                true);
        }

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSON");
            mapUtilsMockedStatic.when(
                () -> MapUtils.getBoolean(Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(1);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(2);
            jsonUtilsMockedStatic.when(() -> JsonUtils.stream(Mockito.any()))
                .thenReturn(
                    new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8))
                        .toList()
                        .stream());

            Assertions.assertThat(((List<?>) JsonFileReadAction.perform(Map.of(), context)).size())
                .isEqualTo(2);
        }
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformReadJSONL() throws JSONException, IOException {
        File file = getFile("sample.jsonl");

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mapUtilsMockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mapUtilsMockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mapUtilsMockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSONL");
            mapUtilsMockedStatic.when(
                () -> MapUtils.getBoolean(Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(null);
            mapUtilsMockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(null);

            for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
                jsonUtilsMockedStatic.when(() -> JsonUtils.read(Mockito.eq(line)))
                    .thenReturn(new JSONObject(line).toMap());
            }

            assertEquals(
                new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)),
                new JSONArray((List<?>) JsonFileReadAction.perform(Map.of(), context)),
                true);
        }

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        try (MockedStatic<JsonUtils> jsonUtilsMockedStatic = Mockito.mockStatic(JsonUtils.class);
            MockedStatic<MapUtils> mockedStatic = Mockito.mockStatic(MapUtils.class)) {

            mockedStatic.when(() -> MapUtils.getRequired(
                Mockito.anyMap(), Mockito.eq(FILE_ENTRY), Mockito.eq(Context.FileEntry.class)))
                .thenReturn(Mockito.mock(Context.FileEntry.class));
            mockedStatic.when(() -> MapUtils.getString(
                Mockito.anyMap(), Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
                .thenReturn("JSONL");
            mockedStatic.when(
                () -> MapUtils.getBoolean(Mockito.anyMap(), Mockito.eq(IS_ARRAY), Mockito.eq(true)))
                .thenReturn(true);
            mockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_NUMBER)))
                .thenReturn(1);
            mockedStatic.when(() -> MapUtils.getInteger(Mockito.anyMap(), Mockito.eq(PAGE_SIZE)))
                .thenReturn(2);

            for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
                jsonUtilsMockedStatic.when(() -> JsonUtils.read(Mockito.eq(line)))
                    .thenReturn(new JSONObject(line).toMap());
            }

            Assertions.assertThat(((List<?>) JsonFileReadAction.perform(Map.of(), context)).size())
                .isEqualTo(2);
        }
    }

    private File getFile(String filename) {
        return new File(JsonFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
