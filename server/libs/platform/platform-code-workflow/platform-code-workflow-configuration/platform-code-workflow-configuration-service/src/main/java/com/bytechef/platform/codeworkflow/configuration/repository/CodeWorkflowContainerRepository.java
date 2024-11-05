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

package com.bytechef.platform.codeworkflow.configuration.repository;

import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface CodeWorkflowContainerRepository extends ListCrudRepository<CodeWorkflowContainer, Long> {

    Optional<CodeWorkflowContainer> findByCodeWorkflowContainerReference(String codeWorkflowContainerReference);

    @Query("""
        SELECT code_workflow_container.* FROM code_workflow_container
        JOIN code_workflow ON code_workflow.code_workflow_container_id = code_workflow_container.id
        WHERE workflow_id = :workflowId
        """)
    Optional<CodeWorkflowContainer> findByWorkflowId(@Param("workflowId") String workflowId);
}
