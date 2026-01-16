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

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * @author Ivica Cardic
 */
@JacksonComponent
public class JsonNullableSerializer extends ValueSerializer<JsonNullable<?>> {

    @Override
    public void serialize(JsonNullable<?> value, JsonGenerator gen, SerializationContext provider)
        throws JacksonException {

        if (value.isPresent()) {
            provider.writeValue(gen, value.get());
        }
    }
}
