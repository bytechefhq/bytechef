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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class TaskStateFacadeImpl implements TaskStateFacade {

    private final StateValueDataStorage stateValueDataStorage;
    private final TaskStateService taskStateService;

    @SuppressFBWarnings("EI")
    TaskStateFacadeImpl(StateValueDataStorage stateValueDataStorage, TaskStateService taskStateService) {
        this.stateValueDataStorage = stateValueDataStorage;
        this.taskStateService = taskStateService;
    }

    @Override
    public void delete(JobResumeId jobResumeId) {
        Optional<Object> storedValueOptional = taskStateService.fetchValue(jobResumeId);

        if (storedValueOptional.isPresent()) {
            stateValueDataStorage.delete(
                getKey(jobResumeId), storedValueOptional.get(), () -> taskStateService.fetchValue(jobResumeId));

            taskStateService.delete(jobResumeId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
        Optional<Object> storedValueOptional = taskStateService.fetchValue(jobResumeId);

        return storedValueOptional.map(storedValue -> (T) stateValueDataStorage.read(getKey(jobResumeId), storedValue));
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public <T> Optional<T> fetchUnlockedValue(JobResumeId jobResumeId) {
        Optional<Object> storedValueOptional = taskStateService.fetchUnlockedValue(jobResumeId);

        return storedValueOptional.map(storedValue -> (T) stateValueDataStorage.read(getKey(jobResumeId), storedValue));
    }

    @Override
    public void save(JobResumeId jobResumeId, Object value) {
        Optional<Object> previousStoredValueOptional = taskStateService.fetchValue(jobResumeId);

        Object storedValue = stateValueDataStorage.write(
            getKey(jobResumeId), value, stateValueDataStorage.fetchJobLocation(jobResumeId.getJobId()),
            previousStoredValueOptional.orElse(null), () -> taskStateService.fetchValue(jobResumeId));

        taskStateService.save(jobResumeId, storedValue);
    }

    private static StateValueDataStorage.Key getKey(JobResumeId jobResumeId) {
        return new StateValueDataStorage.Key(
            StateValueDataStorage.TASK_STATE_COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION,
            String.valueOf(jobResumeId.getJobId()), StateValueDataStorage.toKey(jobResumeId.toString()));
    }
}
