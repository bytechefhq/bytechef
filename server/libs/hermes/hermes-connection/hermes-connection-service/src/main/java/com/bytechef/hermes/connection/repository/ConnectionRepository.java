
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

package com.bytechef.hermes.connection.repository;

import com.bytechef.hermes.connection.domain.Connection;
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
public interface ConnectionRepository
    extends ListPagingAndSortingRepository<Connection, Long>, ListCrudRepository<Connection, Long> {

    List<Connection> findAllByComponentNameInOrderByName(List<String> componentNames);

    List<Connection> findAllByComponentNameAndConnectionVersion(String componentName, int version);

    @Query("""
            SELECT connection.* FROM connection
            JOIN connection_tag ON connection.id = connection_tag.connection_id
            WHERE connection.component_name IN (:componentNames)
            AND connection_tag.tag_id IN (:tagIds)
            ORDER BY connection.name
        """)
    List<Connection> findAllByComponentNamesAndTagIds(
        @Param("componentNames") List<String> componentNames, @Param("tagIds") List<Long> tagIds);

    @Query("""
            SELECT connection.* FROM connection
            JOIN connection_tag ON connection.id = connection_tag.connection_id
            WHERE connection_tag.tag_id IN (:tagIds)
            ORDER BY connection.name
        """)
    List<Connection> findAllByTagIdIn(@Param("tagIds") List<Long> tagIds);
}
