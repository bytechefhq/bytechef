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

package com.bytechef.automation.datasync.repository;

import com.bytechef.automation.datasync.domain.DataSyncElement;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface DataSyncElementRepository extends ListCrudRepository<DataSyncElement, Long> {

    @Query("""
        SELECT * FROM data_sync_element
        WHERE data_sync_id = :dataSyncId
        ORDER BY kind ASC
        """)
    List<DataSyncElement> findAllByDataSyncIdOrderByKindAsc(@Param("dataSyncId") long dataSyncId);
}
