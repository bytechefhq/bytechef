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

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.json.helper.JsonHelper;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_OBJECT_HELPERS + "/stringify")
public class ObjectHelpersStringifyTaskHandler implements TaskHandler<String> {

    private final JsonHelper jsonHelper;

    public ObjectHelpersStringifyTaskHandler(JsonHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    @Override
    public String handle(TaskExecution taskExecution) {
        Object input = taskExecution.getRequired("input");

        return jsonHelper.write(input);
    }
}
