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
 * Exposes the "apply sample output" tool to the Sample Output copilot agent. The generated sample output value is
 * applied on the client (the value is not persisted server-side by this tool), so this tool only validates and echoes
 * it back.
 *
 * @author Ivica Cardic
 */
public class SampleOutputTools {

    private static final Logger log = LoggerFactory.getLogger(SampleOutputTools.class);

    @Tool(
        description = "Apply the complete, updated sample output value to the editor. Pass the entire value as a "
            + "JSON string; the previous sample output is fully replaced.")
    public String updateSampleOutput(
        @ToolParam(description = "The complete updated sample output value as a JSON string") String sampleOutput) {

        try {
            Object parsed = JsonUtils.read(sampleOutput);

            return JsonUtils.write(Map.of("sampleOutput", parsed));
        } catch (RuntimeException exception) {
            log.warn("updateSampleOutput rejected invalid JSON: {}", exception.getMessage());

            return JsonUtils.write(Map.of("error", "Invalid JSON: " + exception.getMessage()));
        }
    }
}
