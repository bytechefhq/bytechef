/*
 * Copyright 2025 ByteChef
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

package com.bytechef.commons.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * @author Ivica Cardic
 */
final class XmlIterator implements Iterator<Map<String, ?>> {

    private final XMLStreamReader xmlStreamReader;
    private final XmlMapper xmlMapper;
    private Map<String, ?> value;
    private int lastToken = -1;

    public XmlIterator(XMLStreamReader xmlStreamReader, XmlMapper xmlMapper) {
        this.xmlStreamReader = xmlStreamReader;
        this.xmlMapper = xmlMapper;

        try {
            lastToken = xmlStreamReader.next();

            if (lastToken != XMLStreamConstants.START_ELEMENT) {
                throw new IllegalArgumentException("Provided stream is not a valid XML");
            }
        } catch (XMLStreamException xmlStreamException) {
            throw new RuntimeException(xmlStreamException);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            lastToken = xmlStreamReader.next();

            if (lastToken == XMLStreamConstants.END_ELEMENT) {
                return false;
            }

            while (lastToken == XMLStreamConstants.CHARACTERS) {
                lastToken = xmlStreamReader.next();
            }

            if (lastToken == XMLStreamConstants.END_ELEMENT) {
                return false;
            }

            value = xmlMapper.readValue(xmlStreamReader, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return value != null;
    }

    @Override
    public Map<String, ?> next() throws NoSuchElementException {
        if (lastToken == XMLStreamConstants.END_ELEMENT) {
            throw new NoSuchElementException();
        }

        return value;
    }
}
