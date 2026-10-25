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

import com.bytechef.commons.data.jdbc.DatabaseDialect;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableStorageServiceImpl implements DataTableStorageService {

    static final String TABLE_NAMES_SQL =
        "SELECT tablename FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    static final String USAGE_SQL =
        "SELECT COALESCE(SUM(pg_total_relation_size(" +
            "(quote_ident(current_schema()) || '.' || quote_ident(tablename))::regclass)), 0) " +
            "FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    private static final Pattern PHYSICAL_TABLE_NAME_PATTERN = Pattern.compile("dt_[a-z0-9_]+");

    private final ApplicationProperties applicationProperties;
    private final DatabaseDialect databaseDialect = new DatabaseDialect();
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableStorageServiceImpl(ApplicationProperties applicationProperties, JdbcTemplate jdbcTemplate) {
        this.applicationProperties = applicationProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DataTableStorageUsage getUsage() {
        long maxSizeBytes = getMaxSizeBytes();
        long usedBytes = getUsedBytes(maxSizeBytes, 0);

        boolean unlimited = maxSizeBytes <= 0;
        double percentage = unlimited ? 0.0 : usedBytes * 100.0 / maxSizeBytes;

        return new DataTableStorageUsage(usedBytes, maxSizeBytes, percentage, unlimited);
    }

    @Override
    public long getTableBytes(String baseName, long environmentId) {
        if (databaseDialect.isH2(jdbcTemplate)) {
            return 0;
        }

        return getLiveTableBytes("dt_" + environmentId + "_" + baseName.toLowerCase(Locale.ROOT));
    }

    @Override
    public void checkLimit(long incomingBytes) {
        long maxSizeBytes = getMaxSizeBytes();

        if (maxSizeBytes <= 0) {
            return;
        }

        long usedBytes = getUsedBytes(maxSizeBytes, incomingBytes);

        if (usedBytes + incomingBytes > maxSizeBytes) {
            throw new DataTableStorageLimitExceededException(usedBytes, maxSizeBytes);
        }
    }

    private long getMaxSizeBytes() {
        return applicationProperties.getDataTable()
            .getMaxSizeBytes();
    }

    private long getUsedBytes(long maxSizeBytes, long incomingBytes) {
        long allocatedBytes = getAllocatedUsageBytes();

        if (maxSizeBytes <= 0 || allocatedBytes + incomingBytes <= maxSizeBytes) {
            return allocatedBytes;
        }

        return Math.min(allocatedBytes, getLiveUsageBytes());
    }

    private long getAllocatedUsageBytes() {
        if (databaseDialect.isH2(jdbcTemplate)) {
            return 0;
        }

        Long usedBytes = jdbcTemplate.queryForObject(USAGE_SQL, Long.class);

        return usedBytes == null ? 0 : usedBytes;
    }

    private long getLiveUsageBytes() {
        List<String> physicalTableNames = jdbcTemplate.queryForList(TABLE_NAMES_SQL, String.class);

        return physicalTableNames.stream()
            .mapToLong(this::getLiveTableBytes)
            .sum();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private long getLiveTableBytes(String physicalTableName) {
        if (!PHYSICAL_TABLE_NAME_PATTERN.matcher(physicalTableName)
            .matches()) {

            throw new IllegalArgumentException("Invalid data table name: " + physicalTableName);
        }

        Long liveBytes = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(pg_column_size(t.*)), 0) FROM \"" + physicalTableName + "\" t", Long.class);

        return liveBytes == null ? 0 : liveBytes;
    }
}
