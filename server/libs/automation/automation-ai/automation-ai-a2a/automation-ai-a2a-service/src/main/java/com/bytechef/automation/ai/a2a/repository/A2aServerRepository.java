/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.ai.a2a.repository;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link A2aServer} entities.
 *
 * @author Ivica Cardic
 */
@Repository
public interface A2aServerRepository extends ListCrudRepository<A2aServer, Long> {

    Optional<A2aServer> findBySecretKey(String secretKey);

    Optional<A2aServer> findByUuidAndEnvironment(UUID uuid, int environment);

    /**
     * Finds all A2A servers of the specified type.
     *
     * @param type the ordinal of the platform type to filter by
     * @return a list of A2A servers with the specified type
     */
    @Query("""
        SELECT * FROM a2a_server
        WHERE type = :type
        ORDER BY id ASC
        """)
    List<A2aServer> findAllByType(@Param("type") int type);
}
