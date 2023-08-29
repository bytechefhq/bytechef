
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    static JsonOperations jsonOperations;

    static {
        try {
            ServiceLoader<JsonOperations> serviceLoader = ServiceLoader.load(JsonOperations.class);

            jsonOperations = serviceLoader.findFirst()
                .orElse(null);
        } catch (ServiceConfigurationError e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
        }

        if (jsonOperations == null && logger.isWarnEnabled()) {
            logger.warn("JsonMapper instance is not available");
        }
    }

    private JsonUtils() {
    }

    public static Object read(InputStream inputStream) {
        return jsonOperations.read(inputStream);
    }

    public static <T> T read(InputStream inputStream, Class<T> valueType) {
        return jsonOperations.read(inputStream, valueType);
    }

    public static Object read(InputStream inputStream, String path) {
        return jsonOperations.read(inputStream, path);
    }

    public static <T> T read(InputStream inputStream, String path, Class<T> valueType) {
        return jsonOperations.read(inputStream, path, valueType);
    }

    public static <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference) {
        return jsonOperations.read(inputStream, path, typeReference);
    }

    public static Object read(String json) {
        return jsonOperations.read(json);
    }

    public static <T> T read(String json, Class<T> valueType) {
        return jsonOperations.read(json, valueType);
    }

    public static <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
        return jsonOperations.read(inputStream, typeReference);
    }

    public static <T> T read(String json, TypeReference<T> typeReference) {
        return jsonOperations.read(json, typeReference);
    }

    public static Object read(String json, String path) {
        return jsonOperations.read(json, path);
    }

    public static <T> T read(String json, String path, Class<T> valueType) {
        return jsonOperations.read(json, path, valueType);
    }

    public static <T> T read(String json, String path, TypeReference<T> typeReference) {
        return jsonOperations.read(json, path, typeReference);
    }

    public static List<?> readList(InputStream inputStream) {
        return jsonOperations.readList(inputStream);
    }

    public static <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
        return jsonOperations.readList(inputStream, elementType);
    }

    public static List<?> readList(InputStream inputStream, String path) {
        return jsonOperations.readList(inputStream, path);
    }

    public static <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
        return jsonOperations.readList(inputStream, path, elementType);
    }

    public static List<?> readList(String json) {
        return jsonOperations.readList(json);
    }

    public static <T> List<T> readList(String json, Class<T> elementType) {
        return jsonOperations.readList(json, elementType);
    }

    public static List<?> readList(String json, String path) {
        return jsonOperations.readList(json, path);
    }

    public static <T> List<T> readList(String json, String path, Class<T> elementType) {
        return jsonOperations.readList(json, path, elementType);
    }

    public static <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
        return jsonOperations.readMap(inputStream, valueType);
    }

    public static Map<String, ?> readMap(InputStream inputStream, String path) {
        return jsonOperations.readMap(inputStream, path);
    }

    public static <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType) {
        return jsonOperations.readMap(inputStream, path, valueType);
    }

    public static Map<String, ?> readMap(String json) {
        return jsonOperations.readMap(json);
    }

    public static <V> Map<String, V> readMap(String json, Class<V> valueType) {
        return jsonOperations.readMap(json, valueType);
    }

    public static Map<String, ?> readMap(String json, String path) {
        return jsonOperations.readMap(json, path);
    }

    public static <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
        return jsonOperations.readMap(json, path, valueType);
    }

    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        return jsonOperations.stream(inputStream);
    }

    public static String write(Object object) {
        return jsonOperations.write(object);
    }

    interface JsonOperations {

        Object read(InputStream inputStream);

        <T> T read(InputStream inputStream, Class<T> valueType);

        <T> T read(InputStream inputStream, TypeReference<T> typeReference);

        Object read(InputStream inputStream, String path);

        <T> T read(InputStream inputStream, String path, Class<T> valueType);

        <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference);

        Object read(String json);

        <T> T read(String json, Class<T> valueType);

        <T> T read(String json, TypeReference<T> typeReference);

        Object read(String json, String path);

        <T> T read(String json, String path, Class<T> valueType);

        <T> T read(String json, String path, TypeReference<T> typeReference);

        List<?> readList(InputStream inputStream);

        <T> List<T> readList(InputStream inputStream, Class<T> elementType);

        List<?> readList(InputStream inputStream, String path);

        <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType);

        List<?> readList(String json);

        <T> List<T> readList(String json, Class<T> elementType);

        List<?> readList(String json, String path);

        <T> List<T> readList(String json, String path, Class<T> elementType);

        <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType);

        Map<String, ?> readMap(InputStream inputStream, String path);

        <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType);

        Map<String, ?> readMap(String json);

        <V> Map<String, V> readMap(String json, Class<V> valueType);

        Map<String, ?> readMap(String json, String path);

        <V> Map<String, V> readMap(String json, String path, Class<V> valueType);

        Stream<Map<String, ?>> stream(InputStream inputStream);

        String write(Object object);
    }
}
