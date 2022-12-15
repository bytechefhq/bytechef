
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

import com.bytechef.atlas.domain.Counter;
import com.bytechef.atlas.repository.CounterRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnProperty(prefix = "bytechef.workflow", name = "persistence.provider", havingValue = "jdbc")
public interface JdbcCounterRepository extends PagingAndSortingRepository<Counter, String>, CounterRepository {

    @Override
    @Query("SELECT value FROM counter WHERE id = :id FOR UPDATE")
    Long findValueByIdForUpdate(@Param("id") String id);

    @Modifying
    @Query("UPDATE counter SET value = :value WHERE id = :id")
    void update(@Param("id") String id, @Param("value") long value);
}
