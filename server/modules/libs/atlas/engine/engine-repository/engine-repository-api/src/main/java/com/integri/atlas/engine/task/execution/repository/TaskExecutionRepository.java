/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.task.execution.repository;

import com.integri.atlas.engine.task.execution.TaskExecution;
import java.util.List;

/**
 * @author Arik Cohen
 */
public interface TaskExecutionRepository {
    /**
     * Creates a new persistent represenation of the given {@link TaskExecution}.
     *
     * @param taskExecution
     */
    void create(TaskExecution taskExecution);

    /**
     * Returns the execution steps of the given job
     *
     * @param jobId
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobIdOrderByCreateTime(String jobId);

    /**
     * Returns the execution steps of the given jobs
     *
     * @param jobIds
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobIdsOrderByCreateTime(List<String> jobIds);

    /**
     * Returns a collection of {@link TaskExecution} instances which belong to the job of the given id.
     *
     * @param jobId
     * @return
     */
    List<TaskExecution> findAllByJobIdOrderByTaskNumber(String jobId);

    /**
     * Returns a collection of {@link TaskExecution} instances which are the children of the given parent id.
     *
     * @param parentId
     * @return
     */
    List<TaskExecution> findAllByParentId(String parentId);

    /**
     * Find a single {@link TaskExecution} instance by its id.
     *
     * @param id
     * @return TaskExecution
     */
    TaskExecution findOne(String id);

    /**
     * Merges the state of the given {@link TaskExecution} instance with its persistent representation and
     * returns the merged version.
     *
     * @param taskExecution
     */
    TaskExecution merge(TaskExecution taskExecution);
}
