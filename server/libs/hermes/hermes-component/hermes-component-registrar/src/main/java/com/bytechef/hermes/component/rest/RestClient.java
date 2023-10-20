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

package com.bytechef.hermes.component.rest;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.utils.HttpClientUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class RestClient {

    public Object execute(ActionDefinition actionDefinition, Context context, TaskExecution taskExecution) {
        Map<String, Object> metadata = actionDefinition.getMetadata();

        //        Map<String, Object> parameters = taskExecution.getParameters();

        return HttpClientUtils.executor()
                .exchange((String) metadata.get("path"), getRequestMethod(metadata))
                .execute(context);
    }

    private static HttpClientUtils.RequestMethod getRequestMethod(Map<String, Object> metadata) {
        return HttpClientUtils.RequestMethod.valueOf((String) metadata.get("requestMethod"));
    }
}
