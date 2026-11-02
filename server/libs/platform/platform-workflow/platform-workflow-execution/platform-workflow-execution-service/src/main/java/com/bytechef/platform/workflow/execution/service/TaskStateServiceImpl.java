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

import com.bytechef.platform.data.storage.domain.DataStorageScope;
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
@Service
@Transactional
public class TaskStateServiceImpl implements TaskStateService {

    private final StateValueDataStorage stateValueDataStorage;
    private final TaskStateRepository taskStateRepository;

    @SuppressFBWarnings("EI")
    TaskStateServiceImpl(StateValueDataStorage stateValueDataStorage, TaskStateRepository taskStateRepository) {
        this.stateValueDataStorage = stateValueDataStorage;
        this.taskStateRepository = taskStateRepository;
    }

    @Override
    public void delete(JobResumeId jobResumeId) {
        taskStateRepository
            .findByJobResumeId(jobResumeId.toString())
            .ifPresent(taskState -> {
                stateValueDataStorage.delete(getKey(jobResumeId), taskState.getValue());

                taskStateRepository.delete(taskState);
            });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
        return taskStateRepository.findByJobResumeId(jobResumeId.toString())
            .map(taskState -> (T) stateValueDataStorage.read(getKey(jobResumeId), taskState.getValue()));
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T> Optional<T> fetchUnlockedValue(JobResumeId jobResumeId) {
        return taskStateRepository.findUnlockedByJobResumeId(jobResumeId.toString())
            .map(taskState -> (T) stateValueDataStorage.read(getKey(jobResumeId), taskState.getValue()));
    }

    @Override
    public void save(JobResumeId jobResumeId, Object value) {
        Optional<TaskState> taskStateOptional = taskStateRepository.findByJobResumeId(jobResumeId.toString());

        Object storedValue = stateValueDataStorage.write(
            getKey(jobResumeId), value, stateValueDataStorage.fetchJobLocation(jobResumeId.getJobId()),
            taskStateOptional
                .map(TaskState::getValue)
                .orElse(null));

        taskStateOptional.ifPresentOrElse(
            taskState -> {
                taskState.setValue(storedValue);

                taskStateRepository.save(taskState);
            },
            () -> taskStateRepository.save(new TaskState(jobResumeId, storedValue)));
    }

    private static StateValueDataStorage.Key getKey(JobResumeId jobResumeId) {
        return new StateValueDataStorage.Key(
            StateValueDataStorage.TASK_STATE_COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION,
            String.valueOf(jobResumeId.getJobId()),
            StateValueDataStorage.toKey(jobResumeId.toString()));
    }
}
