
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

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowCrudRepository;
import com.bytechef.atlas.repository.WorkflowRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Order(5)
@Repository
@ConditionalOnProperty(prefix = "bytechef", name = "workflow-repository.jdbc.enabled", havingValue = "true")
public interface JdbcWorkflowRepository
    extends PagingAndSortingRepository<Workflow, String>, WorkflowRepository, WorkflowCrudRepository {

    @Override
    void deleteById(String id);

    @Override
    default Workflow.SourceType getSourceType() {
        return Workflow.SourceType.JDBC;
    }

    @Override
    Workflow save(Workflow workflow);
}
