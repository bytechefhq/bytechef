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

package com.bytechef.test.jsonasssert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
public class JsonFileAssert {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper() {
        {
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    public static void assertEquals(String filename, Object object) {
        try {
            String value = OBJECT_MAPPER.writeValueAsString(object);

            checkFileExists(filename, value);

            JSONAssert.assertEquals(Files.readString(Paths.get(getUri(filename))), value, true);
        } catch (IOException | JSONException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressFBWarnings("NP")
    private static void checkFileExists(String filename, String value) throws IOException {
        File file = new File("src/test/resources/" + filename).getAbsoluteFile();

        if (!file.exists()) {
            Path path = file.toPath();

            Files.createDirectories(path.getParent());
            Files.writeString(path, value);
        }
    }

    private static URI getUri(String path) throws URISyntaxException {
        ClassLoader classLoader = JsonFileAssert.class.getClassLoader();

        URL url = Validate.notNull(classLoader.getResource(path), "url");

        return url.toURI();
    }
}
