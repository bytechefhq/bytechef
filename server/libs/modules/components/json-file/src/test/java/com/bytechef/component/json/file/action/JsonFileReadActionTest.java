/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.json.file.action;

import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_ENTRY;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.IS_ARRAY;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PAGE_NUMBER;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PAGE_SIZE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PATH;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.json.file.JsonFileComponentHandlerTest;
import com.bytechef.component.json.file.constant.FileType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class JsonFileReadActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformReadJSON() throws JSONException, IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample.json");
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(context.file(file1 -> file1.readToString(Mockito.any(FileEntry.class))))
            .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(parameters.getString(
            Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
            .thenReturn("JSON");
        Mockito
            .when(parameters.getBoolean(Mockito.eq(IS_ARRAY), Mockito.eq(false)))
            .thenReturn(true);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_NUMBER)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_SIZE)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_NUMBER)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_SIZE)))
            .thenReturn(null);
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)).toMap());

        assertEquals(
            new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)),
            new JSONObject((Map<String, ?>) JsonFileReadAction.perform(parameters, parameters, context)),
            true);
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformReadJSONArray() throws JSONException, IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample_array.json");
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(context.file(file1 -> file1.getInputStream(Mockito.any(FileEntry.class))))
            .thenReturn(new FileInputStream(file));
        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(parameters.getString(
            Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
            .thenReturn("JSON");
        Mockito.when(parameters.getBoolean(Mockito.eq(IS_ARRAY), Mockito.eq(true)))
            .thenReturn(true);
        Mockito.when(parameters.getString(Mockito.eq(PATH)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_NUMBER)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_SIZE)))
            .thenReturn(null);
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(
                new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8))
                    .toList()
                    .stream());

        assertEquals(
            new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)),
            new JSONArray((List<?>) JsonFileReadAction.perform(parameters, parameters, context)), true);

        Mockito.when(context.file(file1 -> file1.getInputStream(Mockito.any(FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(parameters.getString(Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
            .thenReturn("JSON");
        Mockito.when(parameters.getBoolean(Mockito.eq(IS_ARRAY), Mockito.eq(true)))
            .thenReturn(true);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_NUMBER)))
            .thenReturn(1);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_SIZE)))
            .thenReturn(2);
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(
                new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8))
                    .toList()
                    .stream());

        Assertions.assertThat(((List<?>) JsonFileReadAction.perform(parameters, parameters, context)).size())
            .isEqualTo(2);
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformReadJSONL() throws JSONException, IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample.jsonl");
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(context.file(file1 -> file1.getInputStream(Mockito.any(FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(parameters.getString(Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
            .thenReturn("JSONL");
        Mockito.when(parameters.getBoolean(Mockito.eq(IS_ARRAY), Mockito.eq(true)))
            .thenReturn(true);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_NUMBER)))
            .thenReturn(null);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_SIZE)))
            .thenReturn(null);

        for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
            Mockito.when(context.json(json -> json.read(Mockito.eq(line))))
                .thenReturn(new JSONObject(line).toMap());
        }

        assertEquals(
            new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)),
            new JSONArray((List<?>) JsonFileReadAction.perform(parameters, parameters, context)), true);

        Mockito.when(context.file(file1 -> file1.getInputStream(Mockito.any(FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Mockito.when(parameters.getRequired(Mockito.eq(FILE_ENTRY), Mockito.eq(FileEntry.class)))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(parameters.getString(Mockito.eq(FILE_TYPE), Mockito.eq(FileType.JSON.name())))
            .thenReturn("JSONL");
        Mockito.when(parameters.getBoolean(Mockito.eq(IS_ARRAY), Mockito.eq(true)))
            .thenReturn(true);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_NUMBER)))
            .thenReturn(1);
        Mockito.when(parameters.getInteger(Mockito.eq(PAGE_SIZE)))
            .thenReturn(2);

        for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
            Mockito.when(context.json(json -> json.read(Mockito.eq(line))))
                .thenReturn(new JSONObject(line).toMap());
        }

        Assertions.assertThat(((List<?>) JsonFileReadAction.perform(parameters, parameters, context)).size())
            .isEqualTo(2);
    }

    private File getFile(String filename) {
        return new File(
            JsonFileComponentHandlerTest.class
                .getClassLoader()
                .getResource("dependencies/json-file/" + filename)
                .getFile());
    }
}
