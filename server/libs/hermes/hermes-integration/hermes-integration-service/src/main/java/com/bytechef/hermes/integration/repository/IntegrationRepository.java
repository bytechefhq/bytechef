
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

package com.bytechef.hermes.integration.repository;

import com.bytechef.hermes.integration.domain.Integration;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationRepository
    extends PagingAndSortingRepository<Integration, Long>, CrudRepository<Integration, Long> {

    Iterable<Integration> findByCategoryIdIn(List<Long> categoryIds);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration_tag.tag_id in (:tagIds)
        """)
    Iterable<Integration> findByTagIdIn(@Param("tagIds") List<Long> tagIds);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration.category_id IN (:categoryIds)
            AND integration_tag.tag_id IN (:tagId)
        """)
    Iterable<Integration> findByCategoryIdsAndTagIds(
        @Param("categoryIds") List<Long> categoryIds, @Param("tagIds") List<Long> tagIds);
}
