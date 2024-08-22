/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.commons.util.constant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @author Ivica Cardic
 */
public class ObjectMapperConstants {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper() {
        {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            disable(SerializationFeature.INDENT_OUTPUT);
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    public static final XmlMapper XML_MAPPER = new XmlMapper() {
        {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            disable(SerializationFeature.INDENT_OUTPUT);
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };
}
