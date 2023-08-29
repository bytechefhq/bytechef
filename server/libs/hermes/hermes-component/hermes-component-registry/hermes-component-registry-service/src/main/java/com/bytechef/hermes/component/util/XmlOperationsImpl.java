
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

import com.bytechef.commons.util.XmlUtils;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public final class XmlOperationsImpl implements com.bytechef.hermes.component.util.XmlUtils.XmlOperations {

    static XmlMapper xmlMapper;

    @Configuration
    static class XmlOperationsConfiguration {

        public XmlOperationsConfiguration(XmlMapper xmlMapper) {
            XmlOperationsImpl.xmlMapper = xmlMapper;
        }
    }

    @Override
    public Map<String, ?> read(InputStream inputStream) {
        return XmlUtils.read(inputStream, xmlMapper);
    }

    @Override
    public <T> Map<String, T> read(InputStream inputStream, Class<T> valueType) {
        return XmlUtils.read(inputStream, valueType, xmlMapper);
    }

    @Override
    public <T> Map<String, T> read(InputStream inputStream, TypeReference<T> valueTypeReference) {
        return XmlUtils.read(inputStream, valueTypeReference.getType(), xmlMapper);
    }

    @Override
    public Map<String, ?> read(String xml) {
        return XmlUtils.read(xml, xmlMapper);
    }

    @Override
    public <T> Map<String, T> read(String xml, Class<T> valueType) {
        return XmlUtils.read(xml, valueType, xmlMapper);
    }

    @Override
    public <T> Map<String, T> read(String xml, TypeReference<T> valueTypeReference) {
        return XmlUtils.read(xml, valueTypeReference.getType(), xmlMapper);
    }

    @Override
    public List<?> readList(InputStream inputStream, String path) {
        return XmlUtils.readList(inputStream, path, xmlMapper);
    }

    @Override
    public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
        return XmlUtils.readList(inputStream, path, elementType, xmlMapper);
    }

    @Override
    public <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference) {
        return XmlUtils.readList(inputStream, path, elementTypeReference.getType(), xmlMapper);
    }

    @Override
    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        return XmlUtils.stream(inputStream, xmlMapper);
    }

    @Override
    public String write(Object object) {
        return XmlUtils.write(object, xmlMapper);
    }

    @Override
    public String write(Object object, String rootName) {
        return XmlUtils.write(object, rootName, xmlMapper);
    }
}
