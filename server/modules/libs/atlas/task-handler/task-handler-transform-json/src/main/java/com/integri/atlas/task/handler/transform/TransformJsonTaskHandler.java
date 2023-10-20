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

package com.integri.atlas.task.handler.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component("transformJson")
public class TransformJsonTaskHandler implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        JsonNode jsonNode = _objectMapper.readTree(taskExecution.getRequiredString("json"));

        if (taskExecution.containsKey("findValue")) {
            return jsonNode.findValue(taskExecution.getString("findValue")).asText();
        }

        if (taskExecution.containsKey("updateValue")) {
            Map<String, String> map = taskExecution.get("updateValue", Map.class);

            return ((ObjectNode) jsonNode).put(map.get("key"), map.get("newValue"));
        }

        if (taskExecution.containsKey("mapKeys")) {
            Map<String, String> map = taskExecution.get("mapKeys", Map.class);
        }

        return null;
    }

    ObjectMapper _objectMapper = new ObjectMapper();
}
