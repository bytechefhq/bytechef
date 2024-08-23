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

package com.bytechef.commons.util;

import static com.bytechef.commons.util.constant.ObjectMapperConstants.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class JsonUtils {

    private static final Configuration CONFIGURATION;

    static {
        CONFIGURATION = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider(OBJECT_MAPPER))
            .mappingProvider(new JacksonMappingProvider(OBJECT_MAPPER))
            .options(EnumSet.noneOf(Option.class))
            .build();
    }

    public static Object read(InputStream inputStream) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(InputStream inputStream, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, valueType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(InputStream inputStream, Type type) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(inputStream, typeFactory.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object read(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);

        return documentContext.read(path, Object.class);
    }

    public static <T> T read(InputStream inputStream, String path, Class<T> valueType) {
        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);

        return documentContext.read(path, valueType);
    }

    public static <T> T read(InputStream inputStream, String path, Type type) {
        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);

        return documentContext.read(path, new TypeTypeRef<>(type));
    }

    public static <T> T read(InputStream inputStream, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);

        return documentContext.read(path, typeRef);
    }

    public static Object read(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, Type type) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(json, typeFactory.constructType(type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object read(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);

        return documentContext.read(path, Object.class);
    }

    public static <T> T read(String json, String path, Class<T> valueType) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);

        return documentContext.read(path, valueType);
    }

    public static <T> T read(String json, String path, Type type) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);

        return documentContext.read(path, new TypeTypeRef<>(type));
    }

    public static <T> T read(String json, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);

        return documentContext.read(path, typeRef);
    }

    public static List<?> readList(InputStream inputStream) {
        return readList(inputStream, Object.class);
    }

    public static <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(inputStream, typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<?> readList(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);

        return documentContext.read(path, new TypeRef<>() {});
    }

    public static <T> List<T> readList(
        InputStream inputStream, String path, Class<T> elementType) {

        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructCollectionType(List.class, elementType)));
    }

    public static List<?> readList(String json) {
        return readList(json, Object.class);
    }

    public static <T> List<T> readList(String json, Class<T> elementType) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(json, typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<?> readList(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);

        return documentContext.read(path, new TypeRef<>() {});
    }

    public static <T> List<T> readList(String json, String path, Class<T> elementType) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructCollectionType(List.class, elementType)));
    }

    public static <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(
                inputStream, typeFactory.constructMapType(Map.class, String.class, valueType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, ?> readMap(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructMapType(Map.class, String.class, Object.class)));
    }

    public static <V> Map<String, V> readMap(
        InputStream inputStream, String path, Class<V> valueType) {

        DocumentContext documentContext = JsonPath.parse(inputStream, CONFIGURATION);
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructMapType(Map.class, String.class, valueType)));
    }

    public static Map<String, ?> readMap(String json) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(
                json, typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(Object.class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <V> Map<String, V> readMap(String json, Class<V> valueType) {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        try {
            return OBJECT_MAPPER.readValue(
                json,
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(valueType)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, ?> readMap(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(Object.class))));
    }

    public static <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
        DocumentContext documentContext = JsonPath.parse(json, CONFIGURATION);
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(valueType))));
    }

    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        try {
            return new JsonParserStream(inputStream, OBJECT_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String write(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String write(Object object, boolean includeNulls) {
        ObjectMapper currentObjectMapper = OBJECT_MAPPER;

        if (includeNulls) {
            currentObjectMapper = currentObjectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.ALWAYS);
        }

        try {
            return currentObjectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeWithDefaultPrettyPrinter(Object object) {
        try {
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("    ", "\n"));

            return OBJECT_MAPPER.writer(printer)
                .writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeWithDefaultPrettyPrinter(Object object, boolean includeNulls) {
        ObjectMapper currentObjectMapper = OBJECT_MAPPER;

        if (includeNulls) {
            currentObjectMapper = currentObjectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.ALWAYS);
        }

        try {
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("    ", "\n"));

            return currentObjectMapper.writer(printer)
                .writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
