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

package com.bytechef.platform.workflow.execution.repository;

import com.bytechef.platform.workflow.execution.domain.LicenceJobUsage;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface LicenceJobUsageRepository extends CrudRepository<LicenceJobUsage, String> {

    @Modifying
    @Query("""
        INSERT INTO licence_job_usage (year_month, count, created_date, created_by, last_modified_date,
            last_modified_by, version)
        VALUES (:yearMonth, 0, now(), 'system', now(), 'system', 0)
        ON CONFLICT (year_month) DO NOTHING
        """)
    void insertIgnore(@Param("yearMonth") String yearMonth);

    @Modifying
    @Query("""
        UPDATE licence_job_usage
        SET count = count + 1, last_modified_date = now(), version = version + 1
        WHERE year_month = :yearMonth AND count < :allowed
        """)
    int incrementIfBelow(@Param("yearMonth") String yearMonth, @Param("allowed") long allowed);

    @Query("SELECT count FROM licence_job_usage WHERE year_month = :yearMonth")
    Long findCount(@Param("yearMonth") String yearMonth);
}
