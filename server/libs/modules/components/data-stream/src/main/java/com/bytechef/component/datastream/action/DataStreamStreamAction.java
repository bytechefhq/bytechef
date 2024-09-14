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

package com.bytechef.component.datastream.action;

import static com.bytechef.component.datastream.constant.DataStreamConstants.CONNECTION_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.DESTINATION;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INPUT_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INSTANCE_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INSTANCE_WORKFLOW_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.JOB_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.SOURCE;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TENANT_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.datastream.constant.DataStreamConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * @author Ivica Cardic
 */
public class DataStreamStreamAction {

    public final SyncActionDefinition actionDefinition;

    public DataStreamStreamAction(Job job, JobLauncher jobLauncher) {
        actionDefinition = new SyncActionDefinition(
            action(DataStreamConstants.STREAM)
                .title("Stream Data")
                .description("Stream large volume of data between source and destination applications.")
                .properties(
                    integer("transformation")
                        .description(
                            "Choose between transformation: simple - define source and destination fields, script - define custom transformation script")
                        .label("Transformation")
                        .options(
                            option("Simple", 1),
                            option("Script", 2))),
            job, jobLauncher);
    }

    public static class SyncActionDefinition extends AbstractActionDefinitionWrapper {

        private final Job job;
        private final JobLauncher jobLauncher;

        public SyncActionDefinition(ActionDefinition actionDefinition, Job job, JobLauncher jobLauncher) {
            super(actionDefinition);

            this.job = job;
            this.jobLauncher = jobLauncher;
        }

        @Override
        public Optional<PerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) this::perform);
        }

        protected Object perform(
            Parameters inputParameters, Map<String, ? extends ParameterConnection> connectionParameters,
            Parameters extensions, ActionContext actionContext) throws Exception {

            ActionContextAware actionContextAware = (ActionContextAware) actionContext;

            JobParameters jobParameters = new JobParameters(
                new HashMap<>() {
                    {
                        put(CONNECTION_PARAMETERS, new JobParameter<>(connectionParameters, Map.class));
                        put(DESTINATION, new JobParameter<>(extensions.getMap(DESTINATION), Map.class));
                        put(INPUT_PARAMETERS, new JobParameter<>(inputParameters, Map.class));

                        if (actionContextAware.getInstanceId() != null) {
                            put(INSTANCE_ID, new JobParameter<>(actionContextAware.getInstanceId(), Long.class));
                        }

                        if (actionContextAware.getInstanceWorkflowId() != null) {
                            put(
                                INSTANCE_WORKFLOW_ID,
                                new JobParameter<>(actionContextAware.getInstanceWorkflowId(), Long.class));
                        }

                        put(JOB_ID, new JobParameter<>(actionContextAware.getJobId(), Long.class));
                        put(SOURCE, new JobParameter<>(extensions.getMap(SOURCE), Map.class));
                        put(TENANT_ID, new JobParameter<>(TenantContext.getCurrentTenantId(), String.class));

                        if (actionContextAware.getType() != null) {
                            put(TYPE, new JobParameter<>(String.valueOf(actionContextAware.getType()), String.class));
                        }
                    }
                });

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            List<Throwable> failureExceptions = jobExecution.getFailureExceptions();

            if (!failureExceptions.isEmpty()) {
                throw new RuntimeException(
                    failureExceptions
                        .stream()
                        .map(Throwable::getMessage)
                        .collect(Collectors.joining(",")));
            }

            return Map.of(
                "endTime", jobExecution.getEndTime(),
                "status", jobExecution.getStatus(),
                "startTime", jobExecution.getStartTime());
        }
    }
}
