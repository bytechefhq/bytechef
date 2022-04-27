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

package com.integri.atlas.task.handler.xml.helper;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class XMLHelper {

    private static final XmlMapper xmlMapper = new XmlMapper();

    @SuppressWarnings("unchecked")
    public Map<String, ?> deserialize(String xml) {
        try {
            return xmlMapper.convertValue(xmlMapper.readTree(xml), Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(String xml, Class<T> clazz) {
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String serialize(Object value) {
        return serialize(value, "root");
    }

    public String serialize(Object value, String rootName) {
        try {
            return xmlMapper.writer().withRootName(rootName).writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        try {
            return new XMLStreamReaderStream(inputStream, xmlMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
