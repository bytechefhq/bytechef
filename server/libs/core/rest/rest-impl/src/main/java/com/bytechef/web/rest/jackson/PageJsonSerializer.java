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

package com.bytechef.web.rest.jackson;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * JSON serializer for [Page].
 *
 * @author Ivica Cardic
 */
@JacksonComponent
public class PageJsonSerializer<T> extends ValueSerializer<Page> {

    /**
     * Method that can be called to ask implementation to serialize values of a type this serializer handles. This
     * serializer replicates pre-`Spring Boot 3.2.0` JSON structure.
     *
     * @param page                 Value to serialize; CANNOT be null.
     * @param jsonGenerator        Generator used to output resulting Json content
     * @param serializationContext Provider that can be used to get serializers for serializing Objects value contains,
     *                             if any.
     */
    @Override
    public void serialize(Page page, JsonGenerator jsonGenerator, SerializationContext serializationContext) {

        jsonGenerator.writeStartObject(); // means start, like '{}'
        jsonGenerator.writePOJOProperty("content", page.getContent());
        jsonGenerator.writeBooleanProperty("empty", page.isEmpty());
        jsonGenerator.writeBooleanProperty("first", page.isFirst());
        jsonGenerator.writeBooleanProperty("last", page.isLast());
        jsonGenerator.writeNumberProperty("number", page.getNumber());
        jsonGenerator.writeNumberProperty("numberOfElements", page.getNumberOfElements());
        jsonGenerator.writeNumberProperty("size", page.getSize());
        jsonGenerator.writeNumberProperty("totalPages", page.getTotalPages());
        jsonGenerator.writeNumberProperty("totalElements", page.getTotalElements());

        // mostly duplicate data (pageable.pageSize -> size, pageable.offset -> number, etc.)
        // need special care in `Spring Boot 3.2.0` if `pageable` object is `unpaged`; otherwise error
        Pageable pageable = page.getPageable();

        if (pageable.isUnpaged()) {
            jsonGenerator.writeStringProperty("pageable", "INSTANCE");
        } else {
            jsonGenerator.writePOJOProperty("pageable", pageable);
        }

        jsonGenerator.writePOJOProperty("sort", page.getSort());

        jsonGenerator.writeEndObject();
    }
}
