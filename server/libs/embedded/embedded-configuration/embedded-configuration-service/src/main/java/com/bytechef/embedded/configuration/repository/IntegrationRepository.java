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

package com.bytechef.embedded.configuration.repository;

import com.bytechef.embedded.configuration.domain.Integration;
import java.util.List;
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
public interface IntegrationRepository
    extends ListPagingAndSortingRepository<Integration, Long>, ListCrudRepository<Integration, Long> {

    List<Integration> findAllByCategoryIdOrderByComponentName(long categoryId);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration.category_id = :categoryId
            AND integration_tag.tag_id = :tagId
            ORDER BY component_name
        """)
    List<Integration> findAllByCategoryIdAndTagIdOrderByComponentName(
        @Param("categoryId") long categoryId, @Param("tagId") long tagId);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration_tag.tag_id = :tagId
            ORDER BY component_name
        """)
    List<Integration> findAllByTagIdOrderByName(@Param("tagId") long tagId);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_instance ON integration.id = integration_instance.integration_id
            WHERE integration_instance.id = :integrationInstanceId
        """)
    Integration findByIntegrationInstanceId(long integrationInstanceId);

    Optional<Integration> findByComponentNameIgnoreCase(String name);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_workflow ON integration.id = integration_workflow.integration_id
            WHERE integration_workflow.workflow_id = :workflowId
        """)
    Optional<Integration> findByWorkflowId(@Param("workflowId") String workflowId);
}
