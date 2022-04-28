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

package com.integri.atlas.task.handler.json.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jayway.jsonpath.TypeRef;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JSONHelper {

    private final ObjectMapper objectMapper;
    private final TypeFactory typeFactory = TypeFactory.defaultInstance();

    public JSONHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object checkJSON(Object object) {
        return checkJSON(object, null);
    }

    public Object checkJSON(Object object, Class<?> clazz) {
        Object result = null;

        if (object != null) {
            if (object instanceof List list) {
                result = list;
            } else if (object instanceof Collection<?> collection) {
                result = new ArrayList<>(collection);
            } else if (object instanceof Map map) {
                result = map;
            } else if (object instanceof String string && (string.startsWith("{") || string.startsWith("["))) {
                if (string.startsWith("{")) {
                    result = read(string, Map.class);
                } else {
                    result = read(string, List.class);
                }
            } else {
                throw new IllegalArgumentException(
                    String.format("%s cannot be converted to JSON compatible format", object)
                );
            }

            List<Class<?>> classes = getClasses(result);

            if (clazz != null && !classes.contains(clazz)) {
                throw new IllegalArgumentException(
                    String.format("%s cannot be converted to JSON compatible format", object)
                );
            }
        }

        return result;
    }

    public <T> List<T> checkJSONArray(Object object, Class<T> itemClass) {
        return checkJSONArray(object, typeFactory.constructType(itemClass));
    }

    public <T> List<T> checkJSONArray(Object object, TypeReference<T> typeReference) {
        return checkJSONArray(object, typeFactory.constructType(typeReference));
    }

    @SuppressWarnings({ "raw", "unchecked" })
    public <T> List<T> checkJSONArray(Object object, JavaType itemType) {
        List<T> items = null;

        if (object != null) {
            if (object instanceof List list) {
                items = list;
            } else if (object instanceof Collection collection) {
                items = new ArrayList<>(collection);
            } else if (object instanceof String string && string.startsWith("[")) {
                items = read(string);
            } else {
                throw new IllegalArgumentException(
                    String.format("%s cannot be converted to JSON compatible format", object)
                );
            }

            if (!allItemsEqual(items, itemType)) {
                throw new IllegalArgumentException(
                    String.format("%s cannot be converted to JSON compatible format", object)
                );
            }
        }

        return items;
    }

    public Map<String, ?> checkJSONObject(Object object) {
        return checkJSONObject(object, (JavaType) null);
    }

    public <T> Map<String, T> checkJSONObject(Object object, Class<T> itemClass) {
        return checkJSONObject(object, typeFactory.constructType(itemClass));
    }

    public <T> Map<String, T> checkJSONObject(Object object, TypeReference<T> typeReference) {
        return checkJSONObject(object, typeFactory.constructType(typeReference));
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> checkJSONObject(Object object, JavaType valueType) {
        Map<String, T> result = null;

        if (object != null) {
            if (object instanceof Map map) {
                result = map;
            } else if (object instanceof String string && string.startsWith("{")) {
                result = read(string);
            } else {
                throw new IllegalArgumentException(
                    String.format("%s cannot be converted to JSON compatible format", object)
                );
            }

            if (!result.isEmpty() && valueType != null) {
                if (!allItemsEqual(result.values(), valueType)) {
                    throw new IllegalArgumentException(
                        String.format("%s cannot be converted to JSON compatible format", object)
                    );
                }
            }
        }

        return result;
    }

    public <T> T read(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String json, TypeReference<T> typeReference) {
        return read(json, typeFactory.constructType(typeReference));
    }

    public <T> T read(String json, JavaType javaType) {
        try {
            return objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(InputStream inputStream, String path) {
        DocumentContext documentContext = JsonPath.parse(inputStream);

        return documentContext.read(path);
    }

    public <T> T read(String json, String path) {
        DocumentContext documentContext = JsonPath.parse(json);

        return documentContext.read(path);
    }

    public <T> T read(String json, String path, TypeRef<T> typeRef) {
        DocumentContext documentContext = JsonPath.parse(json);

        return documentContext.read(path, typeRef);
    }

    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        try {
            return new JSONParserStream(inputStream, objectMapper);
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

    private List<Class<?>> getClasses(Object object) {
        Class<?> resultClass = object.getClass();

        List<Class<?>> classes = ClassUtils.getAllInterfaces(resultClass);

        classes.add(object.getClass());
        return classes;
    }

    private boolean allItemsEqual(Collection<?> items, JavaType itemType) {
        boolean equals = true;

        for (Object item : items) {
            List<Class<?>> classes = getClasses(item);

            if (!classes.contains(itemType.getRawClass())) {
                equals = false;

                break;
            }
        }

        return equals;
    }
}
