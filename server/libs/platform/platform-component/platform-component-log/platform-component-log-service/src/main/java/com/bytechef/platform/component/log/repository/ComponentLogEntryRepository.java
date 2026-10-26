/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.log.repository;

import com.bytechef.platform.component.log.domain.ComponentLogEntry;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ComponentLogEntryRepository extends ListCrudRepository<ComponentLogEntry, Long> {

    @Modifying
    @Query("DELETE FROM component_log_entry WHERE scope = :scope AND job_id = :jobId")
    void deleteByScopeAndJobId(@Param("scope") int scope, @Param("jobId") long jobId);

    boolean existsByScopeAndJobId(int scope, long jobId);

    List<ComponentLogEntry> findAllByScopeAndJobIdOrderById(int scope, long jobId);

    List<ComponentLogEntry> findAllByScopeAndJobIdAndTaskExecutionIdOrderById(
        int scope, long jobId, long taskExecutionId);
}
