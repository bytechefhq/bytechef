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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.execution.domain.Counter;
import com.bytechef.atlas.execution.repository.CounterRepository;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface JdbcCounterRepository
    extends ListPagingAndSortingRepository<Counter, Long>, ListCrudRepository<Counter, Long>,
    CounterRepository {

    @Override
    @Query("SELECT value FROM counter WHERE id = :id FOR UPDATE")
    Optional<Long> findValueByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE counter SET value = :value WHERE id = :id")
    void update(@Param("id") Long id, @Param("value") long value);

    Counter save(Counter counter);
}
