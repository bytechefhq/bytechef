/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface JdbcJobRepository
    extends ListPagingAndSortingRepository<Job, Long>, ListCrudRepository<Job, Long>, JobRepository {

    @Override
    Optional<Job> findById(Long id);

    @Override
    @Query("SELECT COUNT(*) FROM job WHERE status=2 AND end_date >= current_date-1 AND end_date < current_date")
    int countCompletedJobsYesterday();

    @Override
    @Query("SELECT COUNT(*) FROM job WHERE status=2 AND end_date >= current_date")
    int countCompletedJobsToday();

    @Override
    @Query("SELECT count(*) FROM job WHERE status=1")
    int countRunningJobs();

    @Override
    @Query("SELECT * FROM job ORDER BY create_date DESC LIMIT 1")
    Optional<Job> findLastJob();

    @Override
    Optional<Job> findTop1ByWorkflowIdOrderByIdDesc(String workflowId);

    @Override
    @Query("SELECT * FROM job j WHERE j.id = (SELECT job_id FROM task_execution te WHERE te.id=:taskExecutionId)")
    Job findByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);

    Job save(Job job);

    @Modifying
    @Query("UPDATE job SET workflow_id = :newWorkflowId WHERE workflow_id = :curWorkflowId")
    void updateWorkflowId(@Param("curWorkflowId") String curWorkflowId, @Param("newWorkflowId") String newWorkflowId);
}
