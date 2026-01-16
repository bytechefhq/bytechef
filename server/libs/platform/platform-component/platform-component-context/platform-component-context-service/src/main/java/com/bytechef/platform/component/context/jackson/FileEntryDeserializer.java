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

package com.bytechef.platform.component.context.jackson;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.context.FileEntryImpl;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * @author Ivica Cardic
 */
@JacksonComponent
public class FileEntryDeserializer extends ValueDeserializer<FileEntry> {

    @Override
    public FileEntry deserialize(JsonParser jp, DeserializationContext ctxt) {
        JsonNode jsonNode = ctxt.readTree(jp);

        return new FileEntryImpl(
            asText("name", jsonNode), asText("extension", jsonNode), asText("mimeType", jsonNode),
            asText("url", jsonNode));
    }

    private String asText(String fieldName, JsonNode jsonNode) {
        JsonNode fieldJsonNode = jsonNode.get(fieldName);

        if (fieldJsonNode == null) {
            return null;
        }

        return fieldJsonNode.asText();
    }
}
