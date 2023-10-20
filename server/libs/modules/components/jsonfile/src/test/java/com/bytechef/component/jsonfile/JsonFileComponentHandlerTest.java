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

package com.bytechef.component.jsonfile;

import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.test.json.JsonArrayUtils;
import com.bytechef.hermes.component.test.json.JsonObjectUtils;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class JsonFileComponentHandlerTest {

    private static final MockContext context = new MockContext();
    private static final JsonFileComponentHandler jsonFileComponentHandler = new JsonFileComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        AssertUtils.assertEquals("definition/jsonfile_v1.json", new JsonFileComponentHandler().getDefinition());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPerformReadJSON() throws IOException, JSONException {
        File file = getFile("sample.json");

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());
        parameters.set("isArray", false);

        assertEquals(
                JsonObjectUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JsonObjectUtils.of((Map<String, ?>) jsonFileComponentHandler.performRead(context, parameters)),
                true);
    }

    @Test
    public void testPerformReadJSONArray() throws IOException, JSONException {
        File file = getFile("sample_array.json");

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());
        parameters.set("fileType", "JSON");

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) jsonFileComponentHandler.performRead(context, parameters)),
                true);

        parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());
        parameters.set("fileType", "JSON");
        parameters.set("pageNumber", 1);
        parameters.set("pageSize", 2);

        Assertions.assertThat(((List<?>) jsonFileComponentHandler.performRead(context, parameters)).size())
                .isEqualTo(2);
    }

    @Test
    public void testPerformReadJSONL() throws IOException, JSONException {
        File file = getFile("sample.jsonl");

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());
        parameters.set("fileType", "JSONL");

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())),
                JsonArrayUtils.of((List<?>) jsonFileComponentHandler.performRead(context, parameters)),
                true);

        parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());
        parameters.set("fileType", "JSONL");
        parameters.set("pageNumber", 1);
        parameters.set("pageSize", 2);

        Assertions.assertThat(((List<?>) jsonFileComponentHandler.performRead(context, parameters)).size())
                .isEqualTo(2);
    }

    @Test
    public void testPerformWriteJSON() throws IOException, JSONException {
        File file = getFile("sample.json");

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("source", JsonObjectUtils.toMap(Files.contentOf(file, Charset.defaultCharset())));
        parameters.set("fileType", "JSON");

        FileEntry fileEntry = jsonFileComponentHandler.performWrite(context, parameters);

        assertEquals(
                JsonObjectUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JsonObjectUtils.of(context.readFileToString(fileEntry)),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.json");
    }

    @Test
    public void testPerformWriteJSONArray() throws IOException, JSONException {
        File file = getFile("sample_array.json");

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("source", JsonArrayUtils.toList(Files.contentOf(file, Charset.defaultCharset())));
        parameters.set("fileType", "JSON");

        FileEntry fileEntry = jsonFileComponentHandler.performWrite(context, parameters);

        assertEquals(
                JsonArrayUtils.of(Files.contentOf(file, Charset.defaultCharset())),
                JsonArrayUtils.of(context.readFileToString(fileEntry)),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.json");

        parameters.set(FILENAME, "test.json");
        parameters.set("fileType", "JSON");
        parameters.set("source", JsonArrayUtils.toList(Files.contentOf(file, Charset.defaultCharset())));

        fileEntry = jsonFileComponentHandler.performWrite(context, parameters);

        assertThat(fileEntry.getName()).isEqualTo("test.json");
    }

    @Test
    public void testPerformWriteJSONL() throws IOException, JSONException {
        File file = getFile("sample.jsonl");

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                "source",
                JsonArrayUtils.toList(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())));
        parameters.set("fileType", "JSONL");

        FileEntry fileEntry = jsonFileComponentHandler.performWrite(context, parameters);

        assertEquals(
                JsonArrayUtils.ofLines(Files.contentOf(file, Charset.defaultCharset())),
                JsonArrayUtils.ofLines(context.readFileToString(fileEntry)),
                true);

        assertThat(fileEntry.getName()).isEqualTo("file.jsonl");

        parameters.set("filename", "test.jsonl");
        parameters.set("fileType", "JSONL");
        parameters.set(
                "source",
                JsonArrayUtils.toList(Files.contentOf(getFile("sample_array.json"), Charset.defaultCharset())));

        fileEntry = jsonFileComponentHandler.performWrite(context, parameters);

        assertThat(fileEntry.getName()).isEqualTo("test.jsonl");
    }

    private File getFile(String filename) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/" + filename);

        return classPathResource.getFile();
    }
}
