/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.execution.repository;

import com.bytechef.atlas.execution.domain.TaskExecution;
import java.util.List;
import java.util.Optional;

/**
 * @author Arik Cohen
 */
public interface TaskExecutionRepository {

    void deleteById(Long id);

    /**
     * Returns the execution steps of the given job
     *
     * @param jobId
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobIdOrderByCreatedDate(Long jobId);

    /**
     * Returns a collection of {@link TaskExecution} instances which belong to the job of the given id.
     *
     * @param jobId
     * @return
     */
    List<TaskExecution> findAllByJobIdOrderByTaskNumber(Long jobId);

    /**
     * Returns a collection of {@link TaskExecution} instances which are the children of the given parent id.
     *
     * @param parentId
     * @return
     */
    List<TaskExecution> findAllByParentId(Long parentId);

    /**
     * Find a single {@link TaskExecution} instance by its id.
     *
     * @param id
     * @return TaskExecution
     */
    Optional<TaskExecution> findById(long id);

    Optional<TaskExecution> findByIdForUpdate(long id);

    /**
     * Creates a new persistent represenation of the given {@link TaskExecution}.
     *
     * @param taskExecution
     */
    TaskExecution save(TaskExecution taskExecution);
}
