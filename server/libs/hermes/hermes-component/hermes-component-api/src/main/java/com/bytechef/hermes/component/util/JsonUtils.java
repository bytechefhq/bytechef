
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper() {
        {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    static {
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    private JsonUtils() {
    }

    public static <T> T read(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(InputStream inputStream, Class<T> clazz) {
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream);

        return documentContext.read(path);
    }

    public static <T> T read(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json);

        return documentContext.read(path, new TypeRef<T>() {});
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
}
