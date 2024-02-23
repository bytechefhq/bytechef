/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.data.stream.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.platform.component.constant.DataStreamConstants.DATA_STREAM;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(DATA_STREAM + "/v1/sync")
public class DataStreamSyncAction implements TaskHandler<Void> {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sync")
        .title("Sync Data Stream")
        .description("Sync large volume of data between source and destination applications.")
        .properties(
            integer("transformation")
                .description(
                    "Choose between transformation: simple - define source and destination fields, script - define custom transformation script")
                .label("Transformation")
                .options(
                    option("Simple", 1),
                    option("Script", 2)));

    public DataStreamSyncAction(JobLauncher jobLauncher) {
    }

    @Override
    public Void handle(TaskExecution taskExecution) throws TaskExecutionException {
        return null;
    }
}
