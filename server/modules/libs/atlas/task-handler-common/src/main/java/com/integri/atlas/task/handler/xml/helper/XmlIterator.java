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
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author Ivica Cardic
 */
class XmlIterator implements Iterator<Map<String, ?>> {

    private final XMLStreamReader xmlStreamReader;
    private final XmlMapper xmlMapper;
    private Map<String, ?> value;

    public XmlIterator(XMLStreamReader xmlStreamReader, XmlMapper xmlMapper) {
        this.xmlStreamReader = xmlStreamReader;
        this.xmlMapper = xmlMapper;

        try {
            int token = xmlStreamReader.next();

            if (token != XMLStreamConstants.START_ELEMENT) {
                throw new IllegalArgumentException("Provided stream is not a valid XML");
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            int token = xmlStreamReader.next();

            if (token == XMLStreamConstants.END_ELEMENT) {
                value = null;
            } else {
                if (token == XMLStreamConstants.CHARACTERS) {
                    while ((token = xmlStreamReader.next()) == XMLStreamConstants.CHARACTERS);
                }

                if (token == XMLStreamConstants.END_ELEMENT) {
                    value = null;
                } else {
                    value = xmlMapper.readValue(xmlStreamReader, Map.class);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return value != null;
    }

    @Override
    public Map<String, ?> next() {
        return value;
    }
}
