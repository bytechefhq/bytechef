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

package com.bytechef.task.handler.objecthelpers.v1_0;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.commons.json.JsonHelper;
import com.bytechef.task.handler.objecthelpers.ObjectHelpersTaskConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class ObjectHelpersTaskHandler {

    @Component(ObjectHelpersTaskConstants.OBJECT_HELPERS
            + "/"
            + ObjectHelpersTaskConstants.VERSION_1_0
            + "/"
            + ObjectHelpersTaskConstants.JSON_PARSE)
    public static class ObjectHelpersParseTaskHandler implements TaskHandler<Object> {

        private final JsonHelper jsonHelper;

        public ObjectHelpersParseTaskHandler(JsonHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
        }

        @Override
        public Object handle(TaskExecution taskExecution) {
            Object input = taskExecution.getRequired("input");

            return jsonHelper.read((String) input, new TypeReference<>() {});
        }
    }

    @Component(ObjectHelpersTaskConstants.OBJECT_HELPERS
            + "/"
            + ObjectHelpersTaskConstants.VERSION_1_0
            + "/"
            + ObjectHelpersTaskConstants.JSON_STRINGIFY)
    public static class ObjectHelpersStringifyTaskHandler implements TaskHandler<String> {

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
}
