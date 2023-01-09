
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.bytechef.atlas.task.execution.TaskStatus;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.repository.query.Param;

/**
 * @author Arik Cohen
 */
public interface TaskExecutionRepository {
    /**
     * Returns the execution steps of the given job
     *
     * @param jobRef
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobRefOrderByCreatedDate(AggregateReference<Job, Long> jobRef);

    /**
     * Returns the execution steps of the given jobs
     *
     * @param jobRefs
     * @return List<TaskExecution>
     */
    List<TaskExecution> findAllByJobRefInOrderByCreatedDate(List<AggregateReference<Job, Long>> jobRefs);

    /**
     * Returns a collection of {@link TaskExecution} instances which belong to the job of the given id.
     *
     * @param jobRef
     * @return
     */
    List<TaskExecution> findAllByJobRefOrderByTaskNumber(AggregateReference<Job, Long> jobRef);

    /**
     * Returns a collection of {@link TaskExecution} instances which are the children of the given parent id.
     *
     * @param parentRef
     * @return
     */
    List<TaskExecution> findAllByParentRef(AggregateReference<TaskExecution, Long> parentRef);

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

    void updateStatus(long id, TaskStatus taskStatus);

    void updateStatusAndStartTime(
        @Param("id") long id, @Param("status") TaskStatus status, @Param("startTime") LocalDateTime startTime);

    void updateStatusAndEndTime(
        @Param("id") long id, @Param("status") TaskStatus status, @Param("endTime") LocalDateTime endTime);
}
