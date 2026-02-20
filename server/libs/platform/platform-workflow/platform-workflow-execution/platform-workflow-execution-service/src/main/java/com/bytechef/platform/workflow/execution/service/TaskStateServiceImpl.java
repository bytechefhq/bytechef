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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.domain.TaskState;
import com.bytechef.platform.workflow.execution.repository.TaskStateRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service("taskStateService")
@Transactional
public class TaskStateServiceImpl implements TaskStateService {

    private final TaskStateRepository taskStateRepository;

    @SuppressFBWarnings("EI")
    public TaskStateServiceImpl(TaskStateRepository taskStateRepository) {
        this.taskStateRepository = taskStateRepository;
    }

    @Override
    public void delete(JobResumeId jobResumeId) {
        taskStateRepository
            .findByJobResumeId(jobResumeId.toString())
            .ifPresent(taskStateRepository::delete);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
        return taskStateRepository.findByJobResumeId(jobResumeId.toString())
            .map(taskState -> (T) taskState.getValue());
    }

    @Override
    public void save(JobResumeId jobResumeId, Object value) {
        taskStateRepository
            .findByJobResumeId(jobResumeId.toString())
            .ifPresentOrElse(
                taskState -> {
                    taskState.setValue(value);

                    taskStateRepository.save(taskState);
                },
                () -> taskStateRepository.save(new TaskState(jobResumeId, value)));
    }
}
