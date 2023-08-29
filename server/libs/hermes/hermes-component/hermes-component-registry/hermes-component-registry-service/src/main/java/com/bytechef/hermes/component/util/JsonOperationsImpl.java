
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

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class JsonOperationsImpl implements com.bytechef.hermes.component.util.JsonUtils.JsonOperations {

    static ObjectMapper objectMapper;

    @Configuration
    static class XmlOperationsConfiguration {

        public XmlOperationsConfiguration(ObjectMapper objectMapper) {
            JsonOperationsImpl.objectMapper = objectMapper;
        }
    }

    @Override
    public Object read(InputStream inputStream) {
        return JsonUtils.read(inputStream, objectMapper);
    }

    @Override
    public <T> T read(InputStream inputStream, Class<T> valueType) {
        return JsonUtils.read(inputStream, valueType, objectMapper);
    }

    @Override
    public <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
        return JsonUtils.read(inputStream, typeReference.getType(), objectMapper);
    }

    @Override
    public Object read(InputStream inputStream, String path) {
        return JsonUtils.read(inputStream, path, objectMapper);
    }

    @Override
    public <T> T read(InputStream inputStream, String path, Class<T> valueType) {
        return JsonUtils.read(inputStream, path, valueType, objectMapper);
    }

    @Override
    public <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference) {
        return JsonUtils.read(inputStream, path, typeReference.getType(), objectMapper);
    }

    @Override
    public Object read(String json) {
        return JsonUtils.read(json, objectMapper);
    }

    @Override
    public <T> T read(String json, Class<T> valueType) {
        return JsonUtils.read(json, valueType, objectMapper);
    }

    @Override
    public <T> T read(String json, TypeReference<T> typeReference) {
        return JsonUtils.read(json, typeReference.getType(), objectMapper);
    }

    @Override
    public Object read(String json, String path) {
        return JsonUtils.read(json, path, objectMapper);
    }

    @Override
    public <T> T read(String json, String path, Class<T> valueType) {
        return JsonUtils.read(json, path, valueType, objectMapper);
    }

    @Override
    public <T> T read(String json, String path, TypeReference<T> typeReference) {
        return JsonUtils.read(json, path, typeReference.getType(), objectMapper);
    }

    @Override
    public List<?> readList(InputStream inputStream) {
        return JsonUtils.readList(inputStream, objectMapper);
    }

    @Override
    public <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
        return JsonUtils.readList(inputStream, elementType, objectMapper);
    }

    @Override
    public List<?> readList(InputStream inputStream, String path) {
        return JsonUtils.readList(inputStream, path, objectMapper);
    }

    @Override
    public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
        return JsonUtils.readList(inputStream, path, elementType, objectMapper);
    }

    @Override
    public List<?> readList(String json) {
        return JsonUtils.readList(json, objectMapper);
    }

    @Override
    public <T> List<T> readList(String json, Class<T> elementType) {
        return JsonUtils.readList(json, elementType, objectMapper);
    }

    @Override
    public List<?> readList(String json, String path) {
        return JsonUtils.readList(json, path, objectMapper);
    }

    @Override
    public <T> List<T> readList(String json, String path, Class<T> elementType) {
        return JsonUtils.readList(json, path, elementType, objectMapper);
    }

    @Override
    public <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
        return JsonUtils.readMap(inputStream, valueType, objectMapper);
    }

    @Override
    public Map<String, ?> readMap(InputStream inputStream, String path) {
        return JsonUtils.readMap(inputStream, path, objectMapper);
    }

    @Override
    public <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType) {
        return JsonUtils.readMap(inputStream, path, valueType, objectMapper);
    }

    @Override
    public Map<String, ?> readMap(String json) {
        return JsonUtils.readMap(json, objectMapper);
    }

    @Override
    public <V> Map<String, V> readMap(String json, Class<V> valueType) {
        return JsonUtils.readMap(json, valueType, objectMapper);
    }

    @Override
    public Map<String, ?> readMap(String json, String path) {
        return JsonUtils.readMap(json, path, objectMapper);
    }

    @Override
    public <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
        return JsonUtils.readMap(json, path, valueType, objectMapper);
    }

    @Override
    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        return JsonUtils.stream(inputStream, objectMapper);
    }

    @Override
    public String write(Object object) {
        return JsonUtils.write(object, objectMapper);
    }
}
