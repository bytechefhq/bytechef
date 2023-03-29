
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

package com.bytechef.dione.integration.repository;

import com.bytechef.dione.integration.domain.Integration;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationRepository
    extends ListPagingAndSortingRepository<Integration, Long>, ListCrudRepository<Integration, Long> {

    List<Integration> findAllByCategoryIdInOrderByName(List<Long> categoryIds);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration_tag.tag_id in (:tagIds)
            ORDER BY name
        """)
    List<Integration> findAllByTagIdInOrderByName(@Param("tagIds") List<Long> tagIds);

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration.category_id IN (:categoryIds)
            AND integration_tag.tag_id IN (:tagId)
            ORDER BY name
        """)
    List<Integration> findAllByCategoryIdsAndTagIdsOrderByName(
        @Param("categoryIds") List<Long> categoryIds, @Param("tagIds") List<Long> tagIds);
}
