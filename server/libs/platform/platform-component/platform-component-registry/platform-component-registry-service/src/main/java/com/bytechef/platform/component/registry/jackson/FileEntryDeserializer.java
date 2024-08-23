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

package com.bytechef.platform.component.registry.jackson;

import com.bytechef.commons.util.constant.ObjectMapperConstants;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.registry.definition.FileEntryImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

/**
 * @author Ivica Cardic
 */
@JsonComponent
public class FileEntryDeserializer extends JsonDeserializer<FileEntry> {

    static {
        SimpleModule module = new SimpleModule();

        module.addDeserializer(FileEntry.class, new FileEntryDeserializer());

        ObjectMapperConstants.OBJECT_MAPPER.registerModule(module);
    }

    @Override
    public FileEntry deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectCodec objectCodec = jp.getCodec();

        JsonNode jsonNode = objectCodec.readTree(jp);

        return new FileEntryImpl(
            asText("extension", jsonNode), asText("mimeType", jsonNode), asText("name", jsonNode),
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
