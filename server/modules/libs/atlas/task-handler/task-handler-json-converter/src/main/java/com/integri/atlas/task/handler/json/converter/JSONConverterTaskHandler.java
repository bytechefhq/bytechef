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

package com.integri.atlas.task.handler.json.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("jsonConverter")
public class JSONConverterTaskHandler implements TaskHandler<Object> {

    private final JSONHelper jsonHelper;

    public JSONConverterTaskHandler(JSONHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    private enum Operation {
        FROM_JSON,
        TO_JSON,
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));
        Object input = taskExecution.getRequired("input");

        if (operation == Operation.FROM_JSON) {
            return jsonHelper.deserialize((String) input, new TypeReference<>() {});
        } else {
            return jsonHelper.serialize(input);
        }
    }
}
