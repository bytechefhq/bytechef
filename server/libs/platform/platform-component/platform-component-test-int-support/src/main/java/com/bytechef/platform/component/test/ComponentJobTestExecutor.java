/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.test;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.SyncMessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Ivica Cardic
 */
public class ComponentJobTestExecutor {

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskExecutor taskExecutor;
    private final Map<String, TaskHandler<?>> taskHandlerMap;
    private final TaskFileStorageImpl taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ComponentJobTestExecutor(
        ContextService contextService, Evaluator evaluator, JobService jobService, TaskExecutor taskExecutor,
        TaskExecutionService taskExecutionService, Map<String, TaskHandler<?>> taskHandlerMap,
        WorkflowService workflowService) {

        this.contextService = contextService;
        this.taskExecutor = taskExecutor;
        this.evaluator = evaluator;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskHandlerMap = taskHandlerMap;
        this.taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());
        this.workflowService = workflowService;
    }

    public Job execute(String workflowId, Map<String, Object> inputs) {
        return execute(workflowId, inputs, Map.of());
    }

    public Job execute(String workflowId, Map<String, Object> inputs, Map<String, TaskHandler<?>> taskHandlerMap) {
        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, SyncMessageBroker::new, getTaskDispatcherPreSendProcessors(),
            taskExecutionService, taskExecutor,
            MapUtils.concat(this.taskHandlerMap, taskHandlerMap)::get, taskFileStorage, -1, workflowService);

        return jobSyncExecutor.execute(new JobParametersDTO(workflowId, inputs), true);
    }

    private static List<TaskDispatcherPreSendProcessor> getTaskDispatcherPreSendProcessors() {
        return List.of(new TaskDispatcherPreSendProcessorImpl());
    }

    private static class TaskDispatcherPreSendProcessorImpl implements TaskDispatcherPreSendProcessor {

        @Override
        public TaskExecution process(TaskExecution taskExecution) {
            taskExecution.putMetadata(MetadataConstants.TYPE, ModeType.AUTOMATION);

            return taskExecution;
        }

        @Override
        public boolean canProcess(TaskExecution taskExecution) {
            return true;
        }
    }
}
