
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

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class XmlUtils {

    static XmlMapper xmlMapper;

    static {
        ServiceLoader<XmlMapper> loader = ServiceLoader.load(XmlMapper.class);

        xmlMapper = loader.findFirst()
            .orElse(null);

        if (xmlMapper == null) {
            System.err.println("XmlMapper instance is not available");
        }
    }

    private XmlUtils() {
    }

    /**
     *
     * @param inputStream
     * @return
     */
    public static Map<String, ?> read(InputStream inputStream) {
        return xmlMapper.read(inputStream);
    }

    /**
     *
     * @param xml
     * @return
     */
    public static Map<String, ?> read(String xml) {
        return xmlMapper.read(xml);
    }

    /**
     *
     * @param xml
     * @param valueType
     * @return
     * @param <T>
     */
    public static <T> T read(String xml, Class<T> valueType) {
        return xmlMapper.read(xml, valueType);
    }

    /**
     *
     * @param xml
     * @param valueTypeRef
     * @return
     * @param <T>
     */
    public static <T> T read(String xml, TypeReference<T> valueTypeRef) {
        return xmlMapper.read(xml, valueTypeRef);
    }

    /**
     *
     * @param inputStream
     * @param path
     * @return
     * @param <T>
     */
    public static <T> T read(InputStream inputStream, String path) {
        return xmlMapper.read(inputStream, path);
    }

    /**
     *
     * @param inputStream
     * @return
     */
    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        return xmlMapper.stream(inputStream);
    }

    /**
     *
     * @param object
     * @return
     */
    public static String write(Object object) {
        return xmlMapper.write(object);
    }

    /**
     *
     * @param object
     * @param rootName
     * @return
     */
    public static String write(Object object, String rootName) {
        return xmlMapper.write(object, rootName);
    }

    interface XmlMapper {
        Map<String, ?> read(InputStream inputStream);

        Map<String, ?> read(String xml);

        <T> T read(String xml, Class<T> valueType);

        <T> T read(String xml, TypeReference<T> valueTypeRef);

        <T> T read(InputStream inputStream, String path);

        Stream<Map<String, ?>> stream(InputStream inputStream);

        String write(Object object);

        String write(Object object, String rootName);
    }
}
