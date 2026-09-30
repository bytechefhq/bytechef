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

package com.bytechef.platform.data.table.execution.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.internal.DataTableDialect;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableStorageServiceImpl implements DataTableStorageService {

    static final String USAGE_SQL =
        "SELECT COALESCE(SUM(pg_total_relation_size(" +
            "(quote_ident(current_schema()) || '.' || quote_ident(tablename))::regclass)), 0) " +
            "FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    private final ApplicationProperties applicationProperties;
    private final DataTableDialect dataTableDialect = new DataTableDialect();
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableStorageServiceImpl(ApplicationProperties applicationProperties, JdbcTemplate jdbcTemplate) {
        this.applicationProperties = applicationProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DataTableStorageUsage getUsage() {
        long limit = getLimit();
        long used = currentUsageBytes();

        boolean unlimited = limit <= 0;
        double percentage = unlimited ? 0.0 : used * 100.0 / limit;

        return new DataTableStorageUsage(used, limit, percentage, unlimited);
    }

    @Override
    public void checkWithinLimit(long incomingBytes) {
        long limit = getLimit();

        if (limit <= 0) {
            return;
        }

        long used = currentUsageBytes();

        if (used + incomingBytes > limit) {
            throw new DataTableStorageLimitExceededException(used, limit);
        }
    }

    private long getLimit() {
        return applicationProperties.getDataTable()
            .getMaxSizeBytes();
    }

    /**
     * H2 has no per-table byte size to roll up -- {@code pg_total_relation_size} has no equivalent there -- so usage
     * reads as zero on it and a configured limit therefore never trips. That makes data tables usable on H2 rather than
     * failing every insert; it also means an H2 run is no evidence that the limit is enforced. Postgres does the
     * accounting.
     */
    private long currentUsageBytes() {
        if (dataTableDialect.isH2(jdbcTemplate)) {
            return 0;
        }

        Long used = jdbcTemplate.queryForObject(USAGE_SQL, Long.class);

        return used == null ? 0 : used;
    }
}
