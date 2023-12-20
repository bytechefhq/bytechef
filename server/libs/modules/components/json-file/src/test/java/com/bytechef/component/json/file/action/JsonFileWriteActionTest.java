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

package com.bytechef.component.json.file.action;

import static com.bytechef.component.json.file.constant.JsonFileConstants.FILENAME;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.SOURCE;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.component.json.file.JsonFileComponentHandlerTest;
import com.bytechef.component.json.file.constant.JsonFileConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Parameters;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class JsonFileWriteActionTest {

    @Test
    public void testPerformWriteJSON() throws JSONException, IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample.json");
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getString(Mockito.eq(FILENAME)))
            .thenReturn(null);
        Mockito.when(parameters.getString(Mockito.eq(FILE_TYPE), Mockito.eq(JsonFileConstants.FileType.JSON.name())))
            .thenReturn("JSON");
        Mockito.when(parameters.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)).toMap());
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));

        JsonFileWriteAction.perform(parameters, parameters, context);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(
            ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .file(
                file1 -> file1.storeContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()));

        ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

        assertEquals(
            new JSONObject(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8)),
            new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)), true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.json");
    }

    @Test
    public void testPerformWriteJSONArray() throws JSONException, IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample_array.json");
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getString(Mockito.eq(FILENAME)))
            .thenReturn(null);
        Mockito.when(parameters.getString(Mockito.eq(FILE_TYPE), Mockito.eq(JsonFileConstants.FileType.JSON.name())))
            .thenReturn("JSON");
        Mockito.when(parameters.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)).toList());
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));

        JsonFileWriteAction.perform(parameters, parameters, context);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .file(file1 -> file1.storeContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()));

        ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

        assertEquals(
            new JSONArray(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8)),
            new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)),
            true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.json");

        Mockito.reset(context);
        Mockito.reset(parameters);

        Mockito.when(parameters.getString(Mockito.eq(FILENAME)))
            .thenReturn("test.json");
        Mockito.when(parameters.getString(
            Mockito.eq(FILE_TYPE), Mockito.eq(JsonFileConstants.FileType.JSON.name())))
            .thenReturn("JSON");
        Mockito.when(parameters.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)).toList());
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));

        JsonFileWriteAction.perform(parameters, parameters, context);

        Mockito.verify(context)
            .file(file1 -> file1.storeContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class)));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("test.json");
    }

    @Test
    public void testPerformWriteJSONL() throws JSONException, IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getFile("sample.jsonl");
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getString(Mockito.eq(FILENAME)))
            .thenReturn(null);
        Mockito.when(parameters.getString(
            Mockito.eq(FILE_TYPE), Mockito.eq(JsonFileConstants.FileType.JSON.name())))
            .thenReturn("JSONL");
        Mockito.when(parameters.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList());
        Mockito.when(context.json(Mockito.any()))
            .thenReturn(Files.contentOf(file, StandardCharsets.UTF_8));

        for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
            Mockito.when(context.json(json -> json.write(Mockito.eq(new JSONObject(line).toMap()))))
                .thenReturn(line);
        }

        JsonFileWriteAction.perform(parameters, parameters, context);

        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);

        Mockito.verify(context)
            .file(file1 -> file1.storeContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()));

        ByteArrayInputStream byteArrayInputStream = inputStreamArgumentCaptor.getValue();

        assertEquals(
            linesOf(new String(byteArrayInputStream.readAllBytes(), StandardCharsets.UTF_8)),
            linesOf(Files.contentOf(file, StandardCharsets.UTF_8)),
            true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.jsonl");

        Mockito.reset(context);
        Mockito.reset(parameters);

        Mockito.when(parameters.getString(Mockito.eq(FILENAME)))
            .thenReturn("test.jsonl");
        Mockito.when(parameters.getString(
            Mockito.eq(FILE_TYPE), Mockito.eq(JsonFileConstants.FileType.JSON.name())))
            .thenReturn("JSONL");
        Mockito.when(parameters.getRequired(Mockito.eq(SOURCE)))
            .thenReturn(linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList());

        for (String line : Files.linesOf(file, StandardCharsets.UTF_8)) {
            Mockito.when(context.json(json -> json.write(Mockito.eq(new JSONObject(line).toMap()))))
                .thenReturn(line);
        }

        JsonFileWriteAction.perform(parameters, parameters, context);

        Mockito.verify(context)
            .file(file1 -> file1.storeContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class)));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("test.jsonl");
    }

    private File getFile(String filename) {
        return new File(
            JsonFileComponentHandlerTest.class
                .getClassLoader()
                .getResource("dependencies/json-file/" + filename)
                .getFile());
    }

    private static JSONArray linesOf(String jsonl) {
        return new JSONArray("[" + jsonl.replace("\n", ",") + "]");
    }
}
