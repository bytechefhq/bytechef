
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
import com.bytechef.hermes.component.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author Ivica Cardic
 */
public class JsonFileReadActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @BeforeAll
    public static void beforeAll() {
//        ReflectionTestUtils.setField(JsonUtils.class, "jsonMapper", new JsonMapper(new ObjectMapper()));
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(context);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteReadJSON() throws JSONException, IOException {
        File file = getFile("sample.json");

        Mockito.when(context.readFileToString(Mockito.any(Context.FileEntry.class)))
            .thenReturn(java.nio.file.Files.readString(Path.of(file.getAbsolutePath())));

        Map<String, ?> inputParameters = Map.of(
            FILE_ENTRY, Mockito.mock(Context.FileEntry.class),
            FILE_TYPE, "JSON",
            IS_ARRAY, false);

        assertEquals(
            new JSONObject(Files.contentOf(file, StandardCharsets.UTF_8)),
            new JSONObject((Map<String, ?>) JsonFileReadAction.executeRead(context, inputParameters)),
            true);
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testExecuteReadJSONArray() throws JSONException, FileNotFoundException {
        File file = getFile("sample_array.json");

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        Map<String, ?> inputParameters = Map.of(
            FILE_ENTRY, Mockito.mock(Context.FileEntry.class),
            FILE_TYPE, "JSON",
            IS_ARRAY, true);

        assertEquals(
            new JSONArray(Files.contentOf(file, StandardCharsets.UTF_8)),
            new JSONArray((List<?>) JsonFileReadAction.executeRead(context, inputParameters)),
            true);

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        inputParameters = Map.of(
            FILE_ENTRY, Mockito.mock(Context.FileEntry.class),
            FILE_TYPE, "JSON",
            IS_ARRAY, false,
            PAGE_NUMBER, 1,
            PAGE_SIZE, 2);

        Assertions.assertThat(((List<?>) JsonFileReadAction.executeRead(context, inputParameters)).size())
            .isEqualTo(2);
    }

    @Test
    @SuppressFBWarnings("OBL")
    public void testExecuteReadJSONL() throws JSONException, IOException {
        File file = getFile("sample.jsonl");

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        Map<String, ?> inputParameters = Map.of(
            FILE_ENTRY, Mockito.mock(Context.FileEntry.class),
            FILE_TYPE, "JSONL",
            IS_ARRAY, true);

        assertEquals(
            new JSONArray(Files.contentOf(getFile("sample_array.json"), StandardCharsets.UTF_8)),
            new JSONArray((List<?>) JsonFileReadAction.executeRead(context, inputParameters)),
            true);

        Mockito.when(context.getFileStream(Mockito.any(Context.FileEntry.class)))
            .thenReturn(new FileInputStream(file));

        inputParameters = Map.of(
            FILE_ENTRY, Mockito.mock(Context.FileEntry.class),
            FILE_TYPE, "JSONL",
            IS_ARRAY, false,
            PAGE_NUMBER, 1,
            PAGE_SIZE, 2);

        Assertions.assertThat(((List<?>) JsonFileReadAction.executeRead(context, inputParameters)).size())
            .isEqualTo(2);
    }

    private File getFile(String filename) {
        return new File(JsonFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }
}
