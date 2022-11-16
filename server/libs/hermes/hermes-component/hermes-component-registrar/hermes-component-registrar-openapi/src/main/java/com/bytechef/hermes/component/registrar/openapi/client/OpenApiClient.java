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

package com.bytechef.hermes.component.registrar.openapi.client;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Action;
import com.bytechef.hermes.component.http.client.HttpClient;
import com.bytechef.hermes.component.http.client.constants.HttpClientConstants.RequestMethod;
import com.bytechef.hermes.component.impl.ExecutionParametersImpl;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class OpenApiClient {

    private static final HttpClient HTTP_CLIENT = new HttpClient();

    public Object execute(Action action, Context context, TaskExecution taskExecution) throws Exception {
        Map<String, Object> metadata = action.getMetadata();

        WorkflowTask workflowTask = taskExecution.getWorkflowTask();

        // workflowTask.put();

        return HTTP_CLIENT.execute(context, new ExecutionParametersImpl(workflowTask), RequestMethod.valueOf((String)
                metadata.get("requestMethod")));
    }
}
