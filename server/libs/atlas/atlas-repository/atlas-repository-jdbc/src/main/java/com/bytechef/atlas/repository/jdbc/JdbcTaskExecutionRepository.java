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
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnProperty(name = "workflow.persistence.provider", havingValue = "jdbc")
public interface JdbcTaskExecutionRepository
        extends PagingAndSortingRepository<TaskExecution, String>, TaskExecutionRepository {

    List<TaskExecution> findAllByJobOrderByCreatedDate(AggregateReference<Job, String> job);

    List<TaskExecution> findAllByJobInOrderByCreatedDate(List<AggregateReference<Job, String>> jobs);

    List<TaskExecution> findAllByJobOrderByTaskNumber(AggregateReference<Job, String> jobId);

    List<TaskExecution> findAllByParent(AggregateReference<TaskExecution, String> parent);

    @Override
    @Query("SELECT * FROM task_execution WHERE id = :id FOR UPDATE")
    Optional<TaskExecution> findByIdForUpdate(@Param("id") String id);
}
