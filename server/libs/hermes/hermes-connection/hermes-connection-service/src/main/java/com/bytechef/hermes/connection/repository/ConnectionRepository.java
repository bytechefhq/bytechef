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

package com.bytechef.hermes.connection.repository;

import com.bytechef.hermes.connection.domain.Connection;
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
public interface ConnectionRepository
    extends ListPagingAndSortingRepository<Connection, Long>, ListCrudRepository<Connection, Long> {

    List<Connection> findAllByComponentNameAndTypeOrderByName(String componentName, int type);

    List<Connection> findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
        String componentName, int connectionVersion, int type);

    @Query("""
            SELECT connection.* FROM connection
            JOIN connection_tag ON connection.id = connection_tag.connection_id
            WHERE connection.component_name = :componentName
            AND connection.type = :type
            AND connection_tag.tag_id = :tagId
            ORDER BY connection.name
        """)
    List<Connection> findAllByComponentNameAndTagIdAndType(
        @Param("componentName") String componentName, @Param("tagId") long tagId, @Param("type") int type);

    @Query("""
            SELECT connection.* FROM connection
            JOIN connection_tag ON connection.id = connection_tag.connection_id
            WHERE connection.component_name = :componentName
            AND connection.connection_version = :connectionVersion
            AND connection.type = :type
            AND connection_tag.tag_id = :tagId
            ORDER BY connection.name
        """)
    Iterable<Connection> findAllByCNCVTIT(
        @Param("componentName") String componentName, @Param("connectionVersion") int connectionVersion,
        @Param("tagId") long tagId, @Param("type") int type);

    @Query("""
            SELECT connection.* FROM connection
            JOIN connection_tag ON connection.id = connection_tag.connection_id
            WHERE connection.type = :type
            AND connection_tag.tag_id = :tagId
            ORDER BY connection.name
        """)
    List<Connection> findAllByTagIdAndType(@Param("tagId") long tagId, @Param("type") int type);

    List<Connection> findAllByType(int type);
}
