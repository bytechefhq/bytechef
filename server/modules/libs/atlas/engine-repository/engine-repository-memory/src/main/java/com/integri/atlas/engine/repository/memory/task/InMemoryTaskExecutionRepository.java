/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.repository.memory.task;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryTaskExecutionRepository implements TaskExecutionRepository {

    private final Map<String, TaskExecution> executions = new HashMap<>();

    @Override
    public TaskExecution findOne(String aId) {
        TaskExecution taskExecution = executions.get(aId);
        Assert.notNull(taskExecution, "unknown task execution: " + aId);
        return taskExecution;
    }

    @Override
    public List<TaskExecution> findByParentId(String aParentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create(TaskExecution aTaskExecution) {
        Assert.isTrue(
            executions.get(aTaskExecution.getId()) == null,
            "task execution " + aTaskExecution.getId() + " already exists"
        );
        executions.put(aTaskExecution.getId(), aTaskExecution);
    }

    @Override
    public TaskExecution merge(TaskExecution aTaskExecution) {
        executions.put(aTaskExecution.getId(), aTaskExecution);
        return aTaskExecution;
    }

    @Override
    public List<TaskExecution> getExecution(String aJobId) {
        return Collections.unmodifiableList(new ArrayList<>(executions.values()));
    }
}
