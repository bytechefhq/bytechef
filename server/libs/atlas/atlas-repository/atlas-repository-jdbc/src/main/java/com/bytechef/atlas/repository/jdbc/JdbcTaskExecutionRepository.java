
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

package com.bytechef.atlas.repository.jdbc;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.repository.TaskExecutionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.bytechef.atlas.task.execution.TaskStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnProperty(prefix = "bytechef.workflow", name = "persistence.provider", havingValue = "jdbc")
public interface JdbcTaskExecutionRepository
    extends PagingAndSortingRepository<TaskExecution, String>, TaskExecutionRepository {

    List<TaskExecution> findAllByJobRefOrderByCreatedDate(AggregateReference<Job, String> jobRef);

    List<TaskExecution> findAllByJobRefInOrderByCreatedDate(List<AggregateReference<Job, String>> jobRefs);

    List<TaskExecution> findAllByJobRefOrderByTaskNumber(AggregateReference<Job, String> jobRef);

    List<TaskExecution> findAllByParentRef(AggregateReference<TaskExecution, String> parentRef);

    @Override
    @Query("SELECT * FROM task_execution WHERE id = :id FOR UPDATE")
    Optional<TaskExecution> findByIdForUpdate(@Param("id") String id);

    @Override
    @Modifying
    @Query("UPDATE task_execution SET status = :status WHERE id = :id")
    void updateStatus(@Param("id") String id, @Param("status") TaskStatus status);

    @Override
    @Modifying
    @Query("UPDATE task_execution SET status = :status AND start_time = :startTime WHERE id = :id")
    void updateStatusAndStartTime(
        @Param("id") String id, @Param("status") TaskStatus status, @Param("startTime") LocalDateTime startTime);

    @Override
    @Modifying
    @Query("UPDATE task_execution SET status = :status AND end_time = :endTime WHERE id = :id")
    void updateStatusAndEndTime(
        @Param("id") String id, @Param("status") TaskStatus status, @Param("endTime") LocalDateTime endTime);
}
