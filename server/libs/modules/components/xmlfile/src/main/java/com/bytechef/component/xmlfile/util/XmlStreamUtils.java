
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

package com.bytechef.component.xmlfile.util;

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class XmlStreamUtils {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Non null stream reference expected");

        try {
            return new XmlStreamReaderStream(inputStream, XML_MAPPER);
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to stream xml", exception);
        }
    }
}
