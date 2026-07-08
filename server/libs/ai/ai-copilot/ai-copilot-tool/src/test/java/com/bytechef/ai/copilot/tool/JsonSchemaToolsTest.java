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

package com.bytechef.ai.copilot.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class JsonSchemaToolsTest {

    private final JsonSchemaTools jsonSchemaTools = new JsonSchemaTools();

    @Test
    void testUpdateJsonSchemaEchoesValidSchema() {
        String result = jsonSchemaTools.updateJsonSchema("{\"type\":\"object\"}");

        assertTrue(result.contains("\"schema\""));
        assertTrue(JsonUtils.readMap(result)
            .containsKey("schema"));
    }

    @Test
    void testUpdateJsonSchemaRejectsInvalidJson() {
        String result = jsonSchemaTools.updateJsonSchema("{not json");

        assertFalse(JsonUtils.readMap(result)
            .containsKey("schema"));
        assertTrue(result.contains("error"));
    }
}
