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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class JsonHelper {

    private final Configuration configuration;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public JsonHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider(objectMapper))
            .mappingProvider(new JacksonMappingProvider(objectMapper))
            .options(EnumSet.noneOf(Option.class))
            .build();
    }

    public Object read(InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(InputStream inputStream, Class<T> valueType) {
        try {
            return objectMapper.readValue(inputStream, valueType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(InputStream inputStream, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(inputStream, typeFactory.constructType(type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object read(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);

        return documentContext.read(path, Object.class);
    }

    public <T> T read(InputStream inputStream, String path, Class<T> valueType) {
        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);

        return documentContext.read(path, valueType);
    }

    public <T> T read(InputStream inputStream, String path, Type type) {
        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);

        return documentContext.read(path, new TypeTypeRef<>(type));
    }

    public <T> T read(InputStream inputStream, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);

        return documentContext.read(path, typeRef);
    }

    public Object read(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String json, Type type) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(json, typeFactory.constructType(type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Object read(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);

        return documentContext.read(path, Object.class);
    }

    public <T> T read(String json, String path, Class<T> valueType) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);

        return documentContext.read(path, valueType);
    }

    public <T> T read(String json, String path, Type type) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);

        return documentContext.read(path, new TypeTypeRef<>(type));
    }

    public <T> T read(String json, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);

        return documentContext.read(path, typeRef);
    }

    public List<?> readList(InputStream inputStream) {
        return readList(inputStream, Object.class);
    }

    public <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(inputStream, typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<?> readList(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);

        return documentContext.read(path, new TypeRef<>() {});
    }

    public <T> List<T> readList(
        InputStream inputStream, String path, Class<T> elementType) {

        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructCollectionType(List.class, elementType)));
    }

    public List<?> readList(String json) {
        return readList(json, Object.class);
    }

    public <T> List<T> readList(String json, Class<T> elementType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(json, typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public List<?> readList(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);

        return documentContext.read(path, new TypeRef<>() {});
    }

    public <T> List<T> readList(String json, String path, Class<T> elementType) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructCollectionType(List.class, elementType)));
    }

    public <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(
                inputStream, typeFactory.constructMapType(Map.class, String.class, valueType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, ?> readMap(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructMapType(Map.class, String.class, Object.class)));
    }

    public <V> Map<String, V> readMap(
        InputStream inputStream, String path, Class<V> valueType) {

        DocumentContext documentContext = JsonPath.parse(inputStream, configuration);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(typeFactory.constructMapType(Map.class, String.class, valueType)));
    }

    public Map<String, ?> readMap(String json) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(
                json, typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(Object.class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <V> Map<String, V> readMap(String json, Class<V> valueType) {
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        try {
            return objectMapper.readValue(
                json,
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(valueType)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, ?> readMap(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(Object.class))));
    }

    public <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
        DocumentContext documentContext = JsonPath.parse(json, configuration);
        TypeFactory typeFactory = objectMapper.getTypeFactory();

        return documentContext.read(
            path, new TypeTypeRef<>(
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(valueType))));
    }

    public JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        try {
            return new JsonParserStream(inputStream, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String write(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String write(Object object, boolean includeNulls) {
        ObjectMapper currentObjectMapper = objectMapper;

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

    public String writeWithDefaultPrettyPrinter(Object object) {
        try {
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("    ", "\n"));

            return objectMapper.writer(printer)
                .writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String writeWithDefaultPrettyPrinter(Object object, boolean includeNulls) {
        ObjectMapper currentObjectMapper = objectMapper;

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
