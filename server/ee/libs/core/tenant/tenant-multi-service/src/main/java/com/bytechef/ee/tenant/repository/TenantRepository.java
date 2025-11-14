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

package com.bytechef.ee.tenant.repository;

import com.bytechef.ee.tenant.util.TenantUtils;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.domain.Tenant;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public class TenantRepository {

    private final DataSource dataSource;

    @SuppressFBWarnings("EI")
    public TenantRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String findTenantIdByOrganizationName(String organizationName) {
        List<String[]> tenantIds = findTenantIdsByColumnNames(
            tenantId -> "SELECT '%s' FROM %s.organization c WHERE UPPER(u.name) = UPPER('%s')".formatted(
                tenantId, TenantUtils.getDatabaseSchema(tenantId), organizationName),
            1);

        if (tenantIds.size() > 1) {
            throw new RuntimeException(
                "More than one row with the given identifier organizationName=" + organizationName);
        }

        if (!tenantIds.isEmpty()) {
            return tenantIds.getFirst()[0];
        }

        return null;
    }

    public List<String> findTenantIds() {
        List<String> tenants = new ArrayList<>();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet =
                statement.executeQuery(
                    "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE '%s'".formatted(
                        TenantConstants.TENANT_PREFIX + '%'));

            while (resultSet.next()) {
                String schemaName = resultSet.getString(1);

                if (schemaName.equals(TenantConstants.TENANT_PREFIX)) {
                    continue;
                }

                tenants.add(getTenantId(schemaName));
            }
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }

        return tenants;
    }

    public List<String> findTenantIdsByUserEmail(String email) {
        List<String[]> tenantIds = findTenantIdsByColumnNames(
            tenantId -> "SELECT '%s' FROM %s.user u WHERE UPPER(u.email) = UPPER('%s')".formatted(
                tenantId, TenantUtils.getDatabaseSchema(tenantId), email),
            1);

        return tenantIds.stream()
            .map(row -> row[0])
            .collect(Collectors.toList());
    }

    public List<String> findTenantIdsByUserLogin(String login) {
        List<String[]> tenantIds = findTenantIdsByColumnNames(
            tenantId -> "SELECT '%s' FROM %s.user u WHERE UPPER(u.login) = UPPER('%s')".formatted(
                tenantId, TenantUtils.getDatabaseSchema(tenantId), login),
            1);

        return tenantIds.stream()
            .map(row -> row[0])
            .collect(Collectors.toList());
    }

    public List<Tenant> findTenants() {
        List<String[]> rows =
            findTenantIdsByColumnNames(tenantId -> "SELECT '%s', c.id, c.name FROM %s.company c".formatted(
                tenantId, TenantUtils.getDatabaseSchema(tenantId)), 3);

        return rows.stream()
            .map(row -> {
                Tenant tenant = new Tenant();

                tenant.setTenantId(row[0]);
                tenant.setCompanyId(row[1]);
                tenant.setCompanyName(row[2]);

                return tenant;
            })
            .collect(Collectors.toList());
    }

    public void createTenant(String tenantId) {
        String schema = TenantUtils.getDatabaseSchema(tenantId);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA %s".formatted(schema));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String findMaxTenantId() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet =
                statement.executeQuery(
                    "SELECT MAX(schema_name) FROM information_schema.schemata WHERE schema_name LIKE '%s'".formatted(
                        TenantConstants.TENANT_PREFIX + '%'));

            if (resultSet.next()) {
                String schemaName = resultSet.getString(1);

                return getTenantId(schemaName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return "000000";
    }

    public void deleteTenant(String tenantId) {
        String schema = TenantUtils.getDatabaseSchema(tenantId);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA %s CASCADE".formatted(schema));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String findTenantIdByUserActivationKey(String activationKey) {
        List<String[]> tenantIds = findTenantIdsByColumnNames(
            tenantId -> "SELECT '%s' FROM %s.user u WHERE UPPER(u.activation_key) = UPPER('%s')".formatted(
                tenantId, TenantUtils.getDatabaseSchema(tenantId), activationKey),
            1);

        if (tenantIds.size() > 1) {
            throw new RuntimeException(
                "More than one row with the given identifier activationKey=" + activationKey);
        }

        if (!tenantIds.isEmpty()) {
            return tenantIds.getFirst()[0];
        }

        return null;
    }

    public String findTenantIdByUserResetKey(String resetKey) {
        List<String[]> tenantIds = findTenantIdsByColumnNames(
            tenantId -> "SELECT '%s' FROM %s.user u WHERE UPPER(u.reset_key) = UPPER('%s')".formatted(
                tenantId, TenantUtils.getDatabaseSchema(tenantId), resetKey),
            1);

        if (tenantIds.size() > 1) {
            throw new RuntimeException(
                "More than one row with the given identifier resetKey=" + resetKey);
        }

        if (!tenantIds.isEmpty()) {
            return tenantIds.getFirst()[0];
        }

        return null;
    }

    protected List<String[]> findTenantIdsByColumnNames(Function<String, String> query, int columns) {
        List<String[]> rows = new ArrayList<>();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
            List<String> tenantIds = findTenantIds();

            if (tenantIds.isEmpty()) {
                return Collections.emptyList();
            }

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < tenantIds.size(); i++) {
                String tenantId = tenantIds.get(i);

                sb.append(query.apply(tenantId));

                if (i < tenantIds.size() - 1) {
                    sb.append(" UNION ");
                }
            }

            ResultSet resultSet = statement.executeQuery(sb.toString());

            while (resultSet.next()) {
                String[] row = new String[columns];

                for (int i = 1; i <= columns; i++) {
                    row[i - 1] = resultSet.getString(i);
                }

                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return rows;
    }

    private static String getTenantId(String schemaName) {
        if (schemaName == null) {
            return "000000";
        } else {
            return schemaName.replace(TenantConstants.TENANT_PREFIX + "_", "");
        }
    }
}
