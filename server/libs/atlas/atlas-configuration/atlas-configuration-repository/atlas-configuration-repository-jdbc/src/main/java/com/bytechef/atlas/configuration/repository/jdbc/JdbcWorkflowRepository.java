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

package com.bytechef.atlas.configuration.repository.jdbc;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryJdbc;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Order(5)
@Repository
@ConditionalOnWorkflowRepositoryJdbc
public interface JdbcWorkflowRepository
    extends ListPagingAndSortingRepository<Workflow, String>, WorkflowRepository, WorkflowCrudRepository {

    @Override
    void deleteById(String id);

    @Query("SELECT * FROM workflow WHERE type = :type")
    List<Workflow> findAll(@Param("type") int type);

    @Override
    default SourceType getSourceType() {
        return SourceType.JDBC;
    }

    @Override
    Workflow save(Workflow workflow);
}
