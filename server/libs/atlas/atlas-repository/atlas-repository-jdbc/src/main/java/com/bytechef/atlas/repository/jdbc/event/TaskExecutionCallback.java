
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

package com.bytechef.atlas.repository.jdbc.event;

import com.bytechef.atlas.domain.TaskExecution;
import java.time.LocalDateTime;
import org.springframework.core.annotation.Order;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Order(1)
@Component
public class TaskExecutionCallback implements BeforeConvertCallback<TaskExecution> {

    @Override
    public TaskExecution onBeforeConvert(TaskExecution taskExecution) {
        // TODO check why Auditing does not populate auditing fields
        if (taskExecution.isNew()) {
            taskExecution.setCreatedBy("system");
            taskExecution.setCreatedDate(LocalDateTime.now());
            taskExecution.setLastModifiedBy("system");
            taskExecution.setLastModifiedDate(LocalDateTime.now());
        }

        taskExecution.setLastModifiedBy("system");
        taskExecution.setLastModifiedDate(LocalDateTime.now());

        return taskExecution;
    }
}
