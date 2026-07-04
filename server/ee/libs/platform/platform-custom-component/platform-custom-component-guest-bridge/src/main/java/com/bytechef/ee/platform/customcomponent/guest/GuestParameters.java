/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Parameters} implementation used inside the GraalVM Espresso guest JVM. Parameter values arrive from the host
 * as a JSON-decoded map; typed access is implemented with Jackson conversions so component code sees the same coercion
 * behavior for scalars, collections and temporal types.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GuestParameters extends HashMap<String, Object> implements Parameters {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    public GuestParameters(Map<String, ?> map) {
        super(map);
    }

    @Override
    public boolean containsPath(String path) {
        return fromPath(path) != null;
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return convert(get(key), returnType);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        T value = convert(get(key), returnType);

        return value == null ? defaultValue : value;
    }

    @Override
    public Object[] getArray(String key) {
        return convert(get(key), Object[].class);
    }

    @Override
    public Object[] getArray(String key, Object[] defaultValue) {
        Object[] value = convert(get(key), Object[].class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Object[] getArray(String key, List<?> defaultValue) {
        Object[] value = convert(get(key), Object[].class);

        return value == null ? defaultValue.toArray() : value;
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return convertArray(get(key), elementType);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType, T[] defaultValue) {
        T[] value = convertArray(get(key), elementType);

        return value == null ? defaultValue : value;
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType, List<T> defaultValue) {
        T[] value = convertArray(get(key), elementType);

        if (value != null) {
            return value;
        }

        return convertArray(defaultValue, elementType);
    }

    @Override
    public Boolean getBoolean(String key) {
        return convert(get(key), Boolean.class);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = convert(get(key), Boolean.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Date getDate(String key) {
        return convert(get(key), Date.class);
    }

    @Override
    public Date getDate(String key, Date defaultValue) {
        Date value = convert(get(key), Date.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Double getDouble(String key) {
        return convert(get(key), Double.class);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        Double value = convert(get(key), Double.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Duration getDuration(String key) {
        return convert(get(key), Duration.class);
    }

    @Override
    public Duration getDuration(String key, Duration defaultDuration) {
        Duration value = convert(get(key), Duration.class);

        return value == null ? defaultDuration : value;
    }

    @Override
    public FileEntry getFileEntry(String key) {
        throw newFileEntryUnsupportedException();
    }

    @Override
    public List<FileEntry> getFileEntries(String key) {
        throw newFileEntryUnsupportedException();
    }

    @Override
    public List<FileEntry> getFileEntries(String key, List<FileEntry> defaultValue) {
        throw newFileEntryUnsupportedException();
    }

    @Override
    public Float getFloat(String key) {
        return convert(get(key), Float.class);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        Float value = convert(get(key), Float.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public <T> T getFromPath(String path, Class<T> elementType) {
        return convert(fromPath(path), elementType);
    }

    @Override
    public <T> T getFromPath(String path, Class<T> elementType, T defaultValue) {
        T value = convert(fromPath(path), elementType);

        return value == null ? defaultValue : value;
    }

    @Override
    public <T> T getFromPath(String path, TypeReference<T> elementTypeReference) {
        return convert(fromPath(path), elementTypeReference);
    }

    @Override
    public <T> T getFromPath(String path, TypeReference<T> elementTypeReference, T defaultValue) {
        T value = convert(fromPath(path), elementTypeReference);

        return value == null ? defaultValue : value;
    }

    @Override
    public Integer getInteger(String key) {
        return convert(get(key), Integer.class);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        Integer value = convert(get(key), Integer.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public List<?> getList(String key) {
        return convert(get(key), List.class);
    }

    @Override
    public List<?> getList(String key, List<?> defaultValue) {
        List<?> value = convert(get(key), List.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return convertList(get(key), elementType);
    }

    @Override
    public <T> List<T> getList(String key, TypeReference<T> elementTypeReference) {
        return convertList(get(key), elementTypeReference);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        List<T> value = convertList(get(key), elementType);

        return value == null ? defaultValue : value;
    }

    @Override
    public List<?> getList(String key, Class<?>[] elementTypes) {
        return convert(get(key), List.class);
    }

    @Override
    public List<?> getList(String key, List<Class<?>> elementTypes, List<?> defaultValue) {
        List<?> value = convert(get(key), List.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public <T> List<T> getList(String rows, TypeReference<T> typeReference, List<T> defaultValue) {
        List<T> value = convertList(get(rows), typeReference);

        return value == null ? defaultValue : value;
    }

    @Override
    public LocalDate getLocalDate(String key) {
        return convert(get(key), LocalDate.class);
    }

    @Override
    public LocalDate getLocalDate(String key, LocalDate defaultValue) {
        LocalDate value = convert(get(key), LocalDate.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public LocalDateTime getLocalDateTime(String key) {
        return convert(get(key), LocalDateTime.class);
    }

    @Override
    public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
        LocalDateTime value = convert(get(key), LocalDateTime.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public LocalTime getLocalTime(String key) {
        return convert(get(key), LocalTime.class);
    }

    @Override
    public LocalTime getLocalTime(String key, LocalTime defaultValue) {
        LocalTime value = convert(get(key), LocalTime.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Long getLong(String key) {
        return convert(get(key), Long.class);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        Long value = convert(get(key), Long.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Map<String, ?> getMap(String key) {
        return convertMap(get(key), Object.class);
    }

    @Override
    public Map<String, ?> getMap(String key, Map<String, ?> defaultValue) {
        Map<String, ?> value = convertMap(get(key), Object.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public <V> Map<String, V> getMap(String key, Class<V> valueType) {
        return convertMap(get(key), valueType);
    }

    @Override
    public <V> Map<String, V> getMap(String key, TypeReference<V> valueTypeReference) {
        return convertMap(get(key), valueTypeReference);
    }

    @Override
    public <V> Map<String, V> getMap(String key, Class<V> valueType, Map<String, V> defaultValue) {
        Map<String, V> value = convertMap(get(key), valueType);

        return value == null ? defaultValue : value;
    }

    @Override
    public <V> Map<String, V> getMap(String key, TypeReference<V> valueTypeReference, Map<String, V> defaultValue) {
        Map<String, V> value = convertMap(get(key), valueTypeReference);

        return value == null ? defaultValue : value;
    }

    @Override
    public Map<String, ?> getMap(String key, List<Class<?>> valueTypes) {
        return convertMap(get(key), Object.class);
    }

    @Override
    public Map<String, ?> getMap(String key, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {
        Map<String, ?> value = convertMap(get(key), Object.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Map<String, ?> getMapFromPath(String path, List<Class<?>> valueTypes) {
        return convertMap(fromPath(path), Object.class);
    }

    @Override
    public Map<String, ?> getMapFromPath(String path, List<Class<?>> valueTypes, Map<String, ?> defaultValue) {
        Map<String, ?> value = convertMap(fromPath(path), Object.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Object getRequired(String key) {
        return required(key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return convert(required(key), returnType);
    }

    @Override
    public Object[] getRequiredArray(String key) {
        return convert(required(key), Object[].class);
    }

    @Override
    public <T> T[] getRequiredArray(String key, Class<T> elementType) {
        return convertArray(required(key), elementType);
    }

    @Override
    public boolean getRequiredBoolean(String key) {
        return convert(required(key), Boolean.class);
    }

    @Override
    public Date getRequiredDate(String key) {
        return convert(required(key), Date.class);
    }

    @Override
    public double getRequiredDouble(String key) {
        return convert(required(key), Double.class);
    }

    @Override
    public FileEntry getRequiredFileEntry(String key) {
        throw newFileEntryUnsupportedException();
    }

    @Override
    public float getRequiredFloat(String key) {
        return convert(required(key), Float.class);
    }

    @Override
    public <T> T getRequiredFromPath(String path, Class<T> elementType) {
        return convert(requiredFromPath(path), elementType);
    }

    @Override
    public <T> T getRequiredFromPath(String path, TypeReference<T> elementTypeReference) {
        return convert(requiredFromPath(path), elementTypeReference);
    }

    @Override
    public int getRequiredInteger(String key) {
        return convert(required(key), Integer.class);
    }

    @Override
    public List<?> getRequiredList(String key) {
        return convert(required(key), List.class);
    }

    @Override
    public <T> List<T> getRequiredList(String key, Class<T> elementType) {
        return convertList(required(key), elementType);
    }

    @Override
    public <T> List<T> getRequiredList(String key, TypeReference<T> elementTypeReference) {
        return convertList(required(key), elementTypeReference);
    }

    @Override
    public LocalDate getRequiredLocalDate(String key) {
        return convert(required(key), LocalDate.class);
    }

    @Override
    public LocalDateTime getRequiredLocalDateTime(String key) {
        return convert(required(key), LocalDateTime.class);
    }

    @Override
    public LocalTime getRequiredLocalTime(String key) {
        return convert(required(key), LocalTime.class);
    }

    @Override
    public long getRequiredLong(String key) {
        return convert(required(key), Long.class);
    }

    @Override
    public Map<String, ?> getRequiredMap(String key) {
        return convertMap(required(key), Object.class);
    }

    @Override
    public <V> Map<String, V> getRequiredMap(String key, Class<V> valueType) {
        return convertMap(required(key), valueType);
    }

    @Override
    public <V> Map<String, V> getRequiredMap(String key, TypeReference<V> valueTypeReference) {
        return convertMap(required(key), valueTypeReference);
    }

    @Override
    public String getRequiredString(String key) {
        return convert(required(key), String.class);
    }

    @Override
    public String getString(String key) {
        return convert(get(key), String.class);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = convert(get(key), String.class);

        return value == null ? defaultValue : value;
    }

    @Override
    public Map<String, ?> toMap() {
        return this;
    }

    private static <T> T convert(Object value, Class<T> returnType) {
        if (value == null) {
            return null;
        }

        if (returnType.isInstance(value)) {
            return returnType.cast(value);
        }

        return OBJECT_MAPPER.convertValue(value, returnType);
    }

    private static <T> T convert(Object value, TypeReference<T> typeReference) {
        if (value == null) {
            return null;
        }

        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return OBJECT_MAPPER.convertValue(value, typeFactory.constructType(typeReference.getType()));
    }

    private static <T> T[] convertArray(Object value, Class<T> elementType) {
        if (value == null) {
            return null;
        }

        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return OBJECT_MAPPER.convertValue(value, typeFactory.constructArrayType(elementType));
    }

    private static <T> List<T> convertList(Object value, Class<T> elementType) {
        if (value == null) {
            return null;
        }

        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return OBJECT_MAPPER.convertValue(value, typeFactory.constructCollectionType(List.class, elementType));
    }

    private static <T> List<T> convertList(Object value, TypeReference<T> elementTypeReference) {
        if (value == null) {
            return null;
        }

        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return OBJECT_MAPPER.convertValue(
            value,
            typeFactory.constructCollectionType(
                List.class, typeFactory.constructType(elementTypeReference.getType())));
    }

    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> convertMap(Object value, Class<V> valueType) {
        if (value == null) {
            return null;
        }

        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return (Map<String, V>) OBJECT_MAPPER.convertValue(
            value, typeFactory.constructMapType(Map.class, String.class, valueType));
    }

    private static <V> Map<String, V> convertMap(Object value, TypeReference<V> valueTypeReference) {
        if (value == null) {
            return null;
        }

        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

        return OBJECT_MAPPER.convertValue(
            value,
            typeFactory.constructMapType(
                Map.class, typeFactory.constructType(String.class),
                typeFactory.constructType(valueTypeReference.getType())));
    }

    private Object fromPath(String path) {
        Object current = this;

        for (String segment : path.split("\\.")) {
            String name = segment;
            int arrayIndexStart = segment.indexOf('[');

            if (arrayIndexStart >= 0) {
                name = segment.substring(0, arrayIndexStart);
            }

            if (!name.isEmpty()) {
                if (!(current instanceof Map<?, ?> currentMap)) {
                    return null;
                }

                current = currentMap.get(name);
            }

            while (arrayIndexStart >= 0) {
                int arrayIndexEnd = segment.indexOf(']', arrayIndexStart);

                int index = Integer.parseInt(segment.substring(arrayIndexStart + 1, arrayIndexEnd));

                if (!(current instanceof List<?> currentList) || index >= currentList.size()) {
                    return null;
                }

                current = currentList.get(index);

                arrayIndexStart = segment.indexOf('[', arrayIndexEnd);
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    private Object required(String key) {
        Object value = get(key);

        if (value == null) {
            throw new IllegalArgumentException("Required parameter '%s' is missing".formatted(key));
        }

        return value;
    }

    private Object requiredFromPath(String path) {
        Object value = fromPath(path);

        if (value == null) {
            throw new IllegalArgumentException("Required parameter path '%s' is missing".formatted(path));
        }

        return value;
    }

    private static UnsupportedOperationException newFileEntryUnsupportedException() {
        return new UnsupportedOperationException(
            "File entries are not supported in the Espresso custom component sandbox; set " +
                "bytechef.component.custom-component.java-loader=class-loader to use the in-process loader");
    }
}
