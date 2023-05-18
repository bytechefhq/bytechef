
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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class XmlUtils {

    static XmlMapper xmlMapper;

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
     * @param <T>
     */
    public static <T> List<T> readList(InputStream inputStream) {
        return xmlMapper.readList(inputStream);
    }

    /**
     *
     * @param xml
     * @return
     * @param <T>
     */
    public static <T> List<T> readList(String xml) {
        return xmlMapper.readList(xml);
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

        <T> T read(InputStream inputStream, String path);

        <T> List<T> readList(InputStream inputStream);

        <T> List<T> readList(String xml);

        Stream<Map<String, ?>> stream(InputStream inputStream);

        String write(Object object);

        String write(Object object, String rootName);
    }
}
