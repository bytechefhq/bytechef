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

package com.bytechef.commons.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.DefaultPrettyPrinter.Indenter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.type.TypeFactory;

/**
 * @author Ivica Cardic
 */
public class JsonUtils {

    private static final JsonMapper JSON_PATH_JSON_MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .addModule(new Jdk8Module())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .build();

    private static final Configuration JSON_PATH_CONFIGURATION = Configuration.builder()
        .jsonProvider(new JacksonJsonProvider(JSON_PATH_JSON_MAPPER))
        .mappingProvider(new JacksonMappingProvider(JSON_PATH_JSON_MAPPER))
        .options(EnumSet.noneOf(Option.class))
        .build();
    private static ObjectMapper objectMapper;

    public static ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    public static Object read(InputStream inputStream) {
        return objectMapper.readValue(inputStream, Object.class);
    }

    public static <T> T read(InputStream inputStream, Class<T> valueType) {
        return objectMapper.readValue(inputStream, valueType);
    }

    public static <T> T read(InputStream inputStream, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.readValue(inputStream, typeFactory.constructType(type));
    }

    public static <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
        return objectMapper.readValue(inputStream, typeReference);
    }

    public static Object read(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, Object.class);
    }

    public static <T> T read(InputStream inputStream, String path, Class<T> valueType) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, valueType);
    }

    public static <T> T read(InputStream inputStream, String path, Type type) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, new TypeTypeRef<>(type));
    }

    public static <T> T read(InputStream inputStream, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, typeRef);
    }

    public static Object read(String json) {
        return objectMapper.readValue(json, Object.class);
    }

    public static <T> T read(String json, Class<T> valueType) {
        return objectMapper.readValue(json, valueType);
    }

    public static <T> T read(String json, TypeReference<T> typeReference) {
        return objectMapper.readValue(json, typeReference);
    }

    public static <T> T read(String json, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.readValue(json, typeFactory.constructType(type));
    }

    public static Object read(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, Object.class);
    }

    public static <T> T read(String json, String path, Class<T> valueType) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, valueType);
    }

    public static <T> T read(String json, String path, Type type) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, new TypeTypeRef<>(type));
    }

    public static <T> T read(String json, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, typeRef);
    }

    public static List<?> readList(InputStream inputStream) {
        return readList(inputStream, Object.class);
    }

    public static <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(inputStream, typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<?> readList(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, new TypeRef<>() {});
    }

    public static <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);
        com.fasterxml.jackson.databind.type.TypeFactory typeFactory = JSON_PATH_JSON_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructCollectionType(List.class, elementType)));
    }

    public static List<?> readList(String json) {
        return readList(json, Object.class);
    }

    public static <T> List<T> readList(String json, Class<T> elementType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(json, typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<?> readList(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);

        return documentContext.read(path, new TypeRef<>() {});
    }

    public static <T> List<T> readList(String json, String path, Class<T> elementType) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);
        com.fasterxml.jackson.databind.type.TypeFactory typeFactory = JSON_PATH_JSON_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructCollectionType(List.class, elementType)));
    }

    public static <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.readValue(inputStream, typeFactory.constructMapType(Map.class, String.class, valueType));
    }

    public static Map<String, ?> readMap(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructMapType(Map.class, String.class, Object.class)));
    }

    public static <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType) {
        DocumentContext documentContext = JsonPath.parse(inputStream, JSON_PATH_CONFIGURATION);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructMapType(Map.class, String.class, valueType)));
    }

    public static Map<String, ?> readMap(String json) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.readValue(
            json, typeFactory.constructMapType(
                Map.class, typeFactory.constructType(String.class), typeFactory.constructType(Object.class)));
    }

    public static <V> Map<String, V> readMap(String json, Class<V> valueType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return objectMapper.readValue(
            json,
            typeFactory.constructMapType(
                Map.class, typeFactory.constructType(String.class), typeFactory.constructType(valueType)));
    }

    public static Map<String, ?> readMap(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(Object.class))));
    }

    public static <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
        DocumentContext documentContext = JsonPath.parse(json, JSON_PATH_CONFIGURATION);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(valueType))));
    }

    public static JsonNode readTree(String json) {
        return objectMapper.readTree(json);
    }

    @SuppressFBWarnings("EI")
    public static void setObjectMapper(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        try {
            return new JsonParserStream(inputStream, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String write(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeWithDefaultPrettyPrinter(Object object) {
        try {
            Indenter indenter = new DefaultIndenter("    ", "\n");

            return objectMapper.writer()
                .with(
                    new DefaultPrettyPrinter()
                        .withArrayIndenter(indenter)
                        .withObjectIndenter(indenter))
                .writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] writeValueAsBytes(Object value) {
        return objectMapper.writeValueAsBytes(value);
    }

    private static class TypeTypeRef<L> extends TypeRef<L> {

        private final Type type;

        TypeTypeRef(Type type) {
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }
    }
}
