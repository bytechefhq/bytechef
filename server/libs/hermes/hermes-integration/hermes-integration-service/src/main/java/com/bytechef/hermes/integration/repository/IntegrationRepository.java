
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

import com.bytechef.hermes.integration.domain.Category;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.tag.domain.Tag;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationRepository
    extends PagingAndSortingRepository<Integration, Long>, CrudRepository<Integration, Long> {

    @Query("""
            SELECT integration.* FROM integration
            JOIN integration_tag ON integration.id = integration_tag.integration_id
            WHERE integration_tag.tag_id = :#{#tagRef.id}
        """)
    Iterable<Integration> findByTagRef(@Param("tagRef") AggregateReference<Tag, Long> tagRef);

    Iterable<Integration> findByCategoryRef(AggregateReference<Category, Long> categoryRef);
}
