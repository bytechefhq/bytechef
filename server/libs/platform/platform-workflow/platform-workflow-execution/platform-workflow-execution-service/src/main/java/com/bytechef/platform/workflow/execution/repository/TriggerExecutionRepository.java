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

package com.bytechef.platform.workflow.execution.repository;

import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface TriggerExecutionRepository
    extends ListPagingAndSortingRepository<TriggerExecution, Long>, ListCrudRepository<TriggerExecution, Long> {

    @Query("SELECT * FROM trigger_execution WHERE id = :id FOR UPDATE")
    Optional<TriggerExecution> findByIdForUpdate(@Param("id") long id);

    @Query("""
        SELECT distinct trigger_execution.* FROM trigger_execution
        JOIN trigger_execution_job ON trigger_execution_job.trigger_execution_id = trigger_execution.id
        WHERE trigger_execution_job.job_id = :jobId
        """)
    TriggerExecution findByJobId(@Param("jobId") long jobId);
}
