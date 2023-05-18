
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

package com.bytechef.hermes.component.util;

import java.io.InputStream;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class JsonUtils {

    static JsonMapper jsonMapper;

    static {
        ServiceLoader<JsonMapper> loader = ServiceLoader.load(JsonMapper.class);

        jsonMapper = loader.findFirst()
            .orElseThrow(() -> new IllegalStateException("JsonMapper instance is not available"));
    }

    private JsonUtils() {
    }

    /**
     *
     * @param json
     * @return
     * @param <T>
     */
    public static <T> T read(String json) {
        return jsonMapper.read(json);
    }

    /**
     *
     * @param inputStream
     * @param path
     * @return
     * @param <T>
     */
    public static <T> T read(InputStream inputStream, String path) {
        return jsonMapper.read(inputStream, path);
    }

    /**
     *
     * @param json
     * @param path
     * @return
     * @param <T>
     */
    public static <T> T read(String json, String path) {
        return jsonMapper.read(json, path);
    }

    /**
     *
     * @param inputStream
     * @return
     */
    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        return jsonMapper.stream(inputStream);
    }

    /**
     *
     * @param object
     * @return
     */
    public static String write(Object object) {
        return jsonMapper.write(object);
    }

    /**
     *
     */
    interface JsonMapper {

        <T> T read(String json);

        <T> T read(InputStream inputStream, String path);

        <T> T read(String json, String path);

        Stream<Map<String, ?>> stream(InputStream inputStream);

        String write(Object object);
    }
}
