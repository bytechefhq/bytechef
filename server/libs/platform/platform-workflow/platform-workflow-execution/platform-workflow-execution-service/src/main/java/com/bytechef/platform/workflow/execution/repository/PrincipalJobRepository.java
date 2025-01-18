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

import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface PrincipalJobRepository extends ListCrudRepository<PrincipalJob, Long>, CustomPrincipalJobRepository {

    int DEFAULT_PAGE_SIZE = 20;

    Optional<PrincipalJob> findByJobIdAndType(Long jobId, int type);

    Optional<PrincipalJob> findTop1ByPrincipalIdAndTypeOrderByJobIdDesc(long principalId, int type);

    @Query("SELECT job_id FROM principal_job where principal_id = :principalId and type = :type")
    List<Long> findallJobIds(@Param("principalId") long principalId, @Param("type") int type);
}
