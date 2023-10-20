
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
public final class XmlUtils {

    private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    static XmlOperations xmlOperations;

    static {
        try {
            ServiceLoader<XmlOperations> serviceLoader = ServiceLoader.load(XmlOperations.class);

            xmlOperations = serviceLoader.findFirst()
                .orElse(null);
        } catch (ServiceConfigurationError e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
        }

        if (xmlOperations == null && logger.isWarnEnabled()) {
            logger.warn("XmlOperations instance is not available");
        }
    }

    private XmlUtils() {
    }

    public static Map<String, ?> read(InputStream inputStream) {
        return xmlOperations.read(inputStream);
    }

    public static <T> Map<String, T> read(InputStream inputStream, Class<T> valueType) {
        return xmlOperations.read(inputStream, valueType);
    }

    public static <T> Map<String, T> read(InputStream inputStream, TypeReference<T> typeReference) {
        return xmlOperations.read(inputStream, typeReference);
    }

    public static Map<String, ?> read(String xml) {
        return xmlOperations.read(xml);
    }

    public static <T> Map<String, T> read(String xml, Class<T> valueType) {
        return xmlOperations.read(xml, valueType);
    }

    public static <T> Map<String, T> read(String xml, TypeReference<T> typeReference) {
        return xmlOperations.read(xml, typeReference);
    }

    public static List<?> readList(InputStream inputStream, String path) {
        return xmlOperations.readList(inputStream, path);
    }

    public static <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
        return xmlOperations.readList(inputStream, path, elementType);
    }

    public static <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference) {
        return xmlOperations.readList(inputStream, path, elementTypeReference);
    }

    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        return xmlOperations.stream(inputStream);
    }

    public static String write(Object object) {
        return xmlOperations.write(object);
    }

    public static String write(Object object, String rootName) {
        return xmlOperations.write(object, rootName);
    }

    interface XmlOperations {

        Map<String, ?> read(InputStream inputStream);

        <T> Map<String, T> read(InputStream inputStream, Class<T> valueType);

        <T> Map<String, T> read(InputStream inputStream, TypeReference<T> valueTypeReference);

        Map<String, ?> read(String xml);

        <T> Map<String, T> read(String xml, Class<T> valueType);

        <T> Map<String, T> read(String xml, TypeReference<T> valueTypeReference);

        List<?> readList(InputStream inputStream, String path);

        <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType);

        <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference);

        Stream<Map<String, ?>> stream(InputStream inputStream);

        String write(Object object);

        String write(Object object, String rootName);
    }
}
