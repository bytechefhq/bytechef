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

package com.integri.atlas.task.handler.object_.helpers;

import static com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.TASK_OBJECT_HELPERS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskConstants.Operation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_OBJECT_HELPERS)
public class ObjectHelpersTaskHandler implements TaskHandler<Object> {

    private final JSONHelper jsonHelper;

    public ObjectHelpersTaskHandler(JSONHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));
        Object input = taskExecution.getRequired("input");

        if (operation == Operation.JSON_PARSE) {
            return jsonHelper.read((String) input, new TypeReference<>() {});
        } else {
            return jsonHelper.write(input);
        }
    }
}
