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

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Exposes the "apply JSON schema" tool to the JSON Schema Builder copilot agent. The generated schema is applied on the
 * client (the schema is not persisted server-side), so this tool only validates and echoes it back.
 *
 * @author Ivica Cardic
 */
public class JsonSchemaTools {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaTools.class);

    @Tool(
        description = "Apply the complete, updated JSON Schema to the builder. Pass the entire schema object as a "
            + "JSON string; the previous schema is fully replaced.")
    public String updateJsonSchema(
        @ToolParam(description = "The complete updated JSON Schema as a JSON string") String schema) {

        try {
            Object parsed = JsonUtils.read(schema);

            return JsonUtils.write(Map.of("schema", parsed));
        } catch (RuntimeException exception) {
            log.warn("updateJsonSchema rejected invalid schema JSON: {}", exception.getMessage());

            return JsonUtils.write(Map.of("error", "Invalid JSON schema: " + exception.getMessage()));
        }
    }
}
