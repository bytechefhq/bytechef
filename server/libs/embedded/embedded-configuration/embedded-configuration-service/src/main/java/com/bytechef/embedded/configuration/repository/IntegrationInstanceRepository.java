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

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceRepository
    extends ListPagingAndSortingRepository<IntegrationInstance, Long>, ListCrudRepository<IntegrationInstance, Long> {

    @Query("SELECT integration_instance.integration_id FROM integration_instance")
    List<Long> findAllIntegrationId();

    List<IntegrationInstance> findAllByIntegrationIdOrderByName(long integrationId);

    @Query("""
            SELECT integration_instance.* FROM integration_instance
            JOIN integration_instance_tag ON integration_instance.id = integration_instance_tag.integration_instance_id
            WHERE integration_instance.integration_id = :integrationId
            AND integration_instance_tag.tag_id = :tagId
        """)
    List<IntegrationInstance> findAllByIntegrationIdAndTagIdOrderByName(
        @Param("integrationId") long integrationId, @Param("tagId") long tagId);

    @Query("""
            SELECT integration_instance.* FROM integration_instance
            JOIN integration_instance_tag ON integration_instance.id = integration_instance_tag.integration_instance_id
            WHERE integration_instance_tag.tag_id = :tagId
        """)
    List<IntegrationInstance> findAllByTagIdOrderByName(@Param("tagId") long tagId);
}
