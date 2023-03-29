
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
import com.bytechef.atlas.repository.JobRepository;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnProperty(prefix = "bytechef", name = "persistence.provider", havingValue = "jdbc")
public interface JdbcJobRepository
    extends ListPagingAndSortingRepository<Job, Long>, JobRepository, CustomJobRepository {

    @Override
    @Query("SELECT COUNT(*) FROM job WHERE status='COMPLETED' AND end_date >= current_date-1 AND end_date < current_date")
    int countCompletedJobsYesterday();

    @Override
    @Query("SELECT COUNT(*) FROM job WHERE status='COMPLETED' AND end_date >= current_date")
    int countCompletedJobsToday();

    @Override
    @Query("SELECT count(*) FROM job WHERE status='STARTED'")
    int countRunningJobs();

    @Override
    @Query("SELECT * FROM job ORDER BY create_time DESC LIMIT 1")
    Optional<Job> findLatestJob();

    @Override
    @Query("SELECT * FROM job j WHERE j.id = (SELECT job_id FROM task_execution te WHERE te.id=:taskExecutionId)")
    Job findByTaskExecutionId(@Param("taskExecutionId") Long taskExecutionId);
}
