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

package com.bytechef.platform.web.rest.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * JSON serializer for [Page].
 *
 * @author Ivica Cardic
 */
@JsonComponent
public class PageJsonSerializer<T> extends JsonSerializer<Page> {

    /**
     * Method that can be called to ask implementation to serialize values of a type this serializer handles. This
     * serializer replicates pre-`Spring Boot 3.2.0` JSON structure.
     *
     * @param page               Value to serialize; CANNOT be null.
     * @param jsonGenerator      Generator used to output resulting Json content
     * @param serializerProvider Provider that can be used to get serializers for serializing Objects value contains, if
     *                           any.
     */
    public void serialize(Page page, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {

        jsonGenerator.writeStartObject(); // means start, like '{}'
        jsonGenerator.writeObjectField("content", page.getContent());
        jsonGenerator.writeBooleanField("empty", page.isEmpty());
        jsonGenerator.writeBooleanField("first", page.isFirst());
        jsonGenerator.writeBooleanField("last", page.isLast());
        jsonGenerator.writeNumberField("number", page.getNumber());
        jsonGenerator.writeNumberField("numberOfElements", page.getNumberOfElements());
        jsonGenerator.writeNumberField("size", page.getSize());
        jsonGenerator.writeNumberField("totalPages", page.getTotalPages());
        jsonGenerator.writeNumberField("totalElements", page.getTotalElements());

        // mostly duplicate data (pageable.pageSize -> size, pageable.offset -> number, etc.)
        // need special care in `Spring Boot 3.2.0` if `pageable` object is `unpaged`; otherwise error
        Pageable pageable = page.getPageable();

        if (pageable.isUnpaged()) {
            jsonGenerator.writeStringField("pageable", "INSTANCE");
        } else {
            jsonGenerator.writeObjectField("pageable", pageable);
        }

        jsonGenerator.writeObjectField("sort", page.getSort());

        jsonGenerator.writeEndObject();
    }
}
