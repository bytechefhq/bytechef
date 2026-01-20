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

package com.bytechef.test.jsonasssert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class JsonFileAssert {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
//        .changeDefaultVisibility(vc -> vc.with(JsonAutoDetect.Visibility.PUBLIC_ONLY))
        .build();

    public static void assertEquals(String filename, Object object) {
        try {
            String value = OBJECT_MAPPER.writeValueAsString(object);

            checkFileExists(filename, value);

            JSONAssert.assertEquals(Files.readString(Paths.get(getUri(filename))), value, true);
        } catch (IOException | JSONException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkFileExists(String filename, String value) throws IOException {
        File file = new File("src/test/resources/" + filename).getAbsoluteFile();

        if (!file.exists()) {
            Path path = file.toPath();

            Files.createDirectories(Objects.requireNonNull(path.getParent()));
            Files.writeString(path, value);
        }
    }

    private static URI getUri(String path) throws URISyntaxException {
        ClassLoader classLoader = JsonFileAssert.class.getClassLoader();

        URL url = Validate.notNull(classLoader.getResource(path), "url");

        return url.toURI();
    }
}
