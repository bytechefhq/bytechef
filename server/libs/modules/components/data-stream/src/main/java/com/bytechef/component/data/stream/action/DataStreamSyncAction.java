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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import java.util.Map;
import java.util.Optional;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * @author Ivica Cardic
 */
public class DataStreamSyncAction {

    public final SyncActionDefinition actionDefinition;

    public DataStreamSyncAction(JobLauncher jobLauncher) {
        actionDefinition = new SyncActionDefinition(
            action("sync")
                .title("Sync Data Stream")
                .description("Sync large volume of data between source and destination applications.")
                .properties(
                    integer("transformation")
                        .description(
                            "Choose between transformation: simple - define source and destination fields, script - define custom transformation script")
                        .label("Transformation")
                        .options(
                            option("Simple", 1),
                            option("Script", 2))),
            jobLauncher);
    }

    public static class SyncActionDefinition extends AbstractActionDefinitionWrapper {

        public SyncActionDefinition(ActionDefinition actionDefinition, JobLauncher jobLauncher) {
            super(actionDefinition);
        }

        @Override
        public Optional<PerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) this::perform);
        }

        protected Object perform(
            Parameters inputParameters, Map<String, ? extends ParameterConnection> connectionParameters,
            ActionContext actionContext) {

            return null;
        }
    }
}
