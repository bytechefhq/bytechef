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

package com.bytechef.atlas.repository;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

/**
 * @author Arik Cohen
 */
public interface TaskExecutionRepository {
    /**
     * Returns the execution steps of the given job
     *
     * @param job
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobOrderByCreatedDate(AggregateReference<Job, String> job);

    /**
     * Returns the execution steps of the given jobs
     *
     * @param jobs
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobInOrderByCreatedDate(List<AggregateReference<Job, String>> jobs);

    /**
     * Returns a collection of {@link TaskExecution} instances which belong to the job of the given
     * id.
     *
     * @param job
     * @return
     */
    List<TaskExecution> findAllByJobOrderByTaskNumber(AggregateReference<Job, String> job);

    /**
     * Returns a collection of {@link TaskExecution} instances which are the children of the given
     * parent id.
     *
     * @param parentId
     * @return
     */
    List<TaskExecution> findAllByParent(AggregateReference<TaskExecution, String> parentId);

    /**
     * Find a single {@link TaskExecution} instance by its id.
     *
     * @param id
     * @return TaskExecution
     */
    Optional<TaskExecution> findById(String id);

    Optional<TaskExecution> findByIdForUpdate(String id);

    /**
     * Creates a new persistent represenation of the given {@link TaskExecution}.
     *
     * @param taskExecution
     */
    TaskExecution save(TaskExecution taskExecution);
}
