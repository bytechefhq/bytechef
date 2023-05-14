
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

/**
 * @author Ivica Cardic
 */
public final class XmlUtils {

    static XmlMapper xmlMapper;

    public static Map<String, ?> read(InputStream inputStream) {
        return xmlMapper.read(inputStream);
    }

    public static Map<String, ?> read(String xml) {
        return xmlMapper.read(xml);
    }

    public static <T> T read(InputStream inputStream, String path) {
        return xmlMapper.read(inputStream, path);
    }

    public static <T> List<T> readList(InputStream inputStream) {
        return xmlMapper.readList(inputStream);
    }

    public static <T> List<T> readList(String xml) {
        return xmlMapper.readList(xml);
    }

    public static String write(Object object) {
        return xmlMapper.write(object);
    }

    public static String write(Object object, String rootName) {
        return xmlMapper.write(object, rootName);
    }

    interface XmlMapper {
        Map<String, ?> read(InputStream inputStream);

        Map<String, ?> read(String xml);

        <T> T read(InputStream inputStream, String path);

        <T> List<T> readList(InputStream inputStream);

        <T> List<T> readList(String xml);

        String write(Object object);

        String write(Object object, String rootName);
    }
}
