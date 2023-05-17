
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
import com.bytechef.component.jsonfile.constant.JsonFileTaskConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.util.JsonMapper;
import com.bytechef.hermes.component.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILENAME;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.SOURCE;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author Ivica Cardic
 */
public class JsonFileWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @BeforeAll
    public static void beforeAll() {
        ReflectionTestUtils.setField(JsonUtils.class, "jsonMapper", new JsonMapper(new ObjectMapper()));
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    public void testExecuteWriteJSON() throws JSONException {
        File file = getFile("sample.json");

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILE_TYPE, JsonFileTaskConstants.FileType.JSON.name()))
            .thenReturn("JSON");
        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)).toMap());

        JsonFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertEquals(
            new JSONObject(new String(inputStreamArgumentCaptor.getValue()
                .readAllBytes(), StandardCharsets.UTF_8)),
            new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)),
            true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.json");
    }

    @Test
    public void testExecuteWriteJSONArray() throws JSONException {
        File file = getFile("sample_array.json");

        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILE_TYPE, JsonFileTaskConstants.FileType.JSON.name()))
            .thenReturn("JSON");
        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)).toList());

        JsonFileWriteAction.executeWrite(context, inputParameters);

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

        Mockito.reset(context);

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILENAME))
            .thenReturn("test.json");
        Mockito.when(inputParameters.getString(FILE_TYPE, JsonFileTaskConstants.FileType.JSON.name()))
            .thenReturn("JSONL");
        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)).toList());

        JsonFileWriteAction.executeWrite(context, inputParameters);

        filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("test.json");
    }

    @Test
    public void testExecuteWriteJSONL() throws JSONException {
        File file = getFile("sample.jsonl");
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILE_TYPE, JsonFileTaskConstants.FileType.JSON.name()))
            .thenReturn("JSONL");
        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(
                linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList());

        JsonFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertEquals(
            linesOf(new String(inputStreamArgumentCaptor.getValue()
                .readAllBytes(), StandardCharsets.UTF_8)),
            linesOf(Files.contentOf(file, StandardCharsets.UTF_8)),
            true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.jsonl");

        Mockito.reset(context);

        inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILENAME))
            .thenReturn("test.jsonl");
        Mockito.when(inputParameters.getString(FILE_TYPE, JsonFileTaskConstants.FileType.JSON.name()))
            .thenReturn("JSONL");
        Mockito.when(inputParameters.getRequired(SOURCE))
            .thenReturn(
                linesOf(Files.contentOf(file, StandardCharsets.UTF_8)).toList());

        JsonFileWriteAction.executeWrite(context, inputParameters);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class));

        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("test.jsonl");
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
