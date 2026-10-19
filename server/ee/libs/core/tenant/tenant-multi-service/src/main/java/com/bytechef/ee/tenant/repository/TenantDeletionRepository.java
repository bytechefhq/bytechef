/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.repository;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.domain.TenantDeletion.Status;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@code public.tenant_deletion} table. The table lives in the public schema so a deletion record
 * outlives the tenant schema it describes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public class TenantDeletionRepository {

    private static final int STATUS_PENDING_DELETION = Status.PENDING_DELETION.ordinal();
    private static final int STATUS_PURGED = Status.PURGED.ordinal();
    private static final int STATUS_CANCELLED = Status.CANCELLED.ordinal();

    private static final String SELECT_COLUMNS =
        "SELECT tenant_id, status, requested_by, requested_date, purge_after, stripe_customer_id, " +
            "stripe_subscription_id, purge_claimed_date, teardown_failures FROM public.tenant_deletion ";

    private final DataSource dataSource;

    @SuppressFBWarnings("EI")
    public TenantDeletionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean claimForPurge(String tenantId, Instant now) {
        String sql = "UPDATE public.tenant_deletion SET purge_claimed_date = ? WHERE tenant_id = ? AND status = ? " +
            "AND (purge_claimed_date IS NULL OR purge_claimed_date < ?::timestamp - interval '1 hour')";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, toLocalDateTime(now));
            preparedStatement.setString(2, tenantId);
            preparedStatement.setInt(3, STATUS_PENDING_DELETION);
            preparedStatement.setObject(4, toLocalDateTime(now));

            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public Optional<TenantDeletion> fetchTenantDeletion(String tenantId) {
        String sql = SELECT_COLUMNS + "WHERE tenant_id = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, tenantId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(toTenantDeletion(resultSet));
                }

                return Optional.empty();
            }
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public Set<String> findInactiveTenantIds() {
        String sql = "SELECT tenant_id FROM public.tenant_deletion WHERE status IN (?, ?)";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, STATUS_PENDING_DELETION);
            preparedStatement.setInt(2, STATUS_PURGED);

            Set<String> tenantIds = new HashSet<>();

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    tenantIds.add(resultSet.getString(1));
                }
            }

            return tenantIds;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public List<TenantDeletion> getDueForPurge(Instant now) {
        String sql = SELECT_COLUMNS + "WHERE status = ? AND purge_after <= ? ORDER BY purge_after";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, STATUS_PENDING_DELETION);
            preparedStatement.setObject(2, toLocalDateTime(now));

            List<TenantDeletion> tenantDeletions = new ArrayList<>();

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    tenantDeletions.add(toTenantDeletion(resultSet));
                }
            }

            return tenantDeletions;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public boolean markPendingDeletion(String tenantId, String requestedBy, Instant purgeAfter) {
        String sql = "INSERT INTO public.tenant_deletion (tenant_id, status, requested_by, requested_date, " +
            "purge_after, teardown_failures) VALUES (?, ?, ?, ?, ?, '[]') ON CONFLICT (tenant_id) DO UPDATE SET " +
            "status = EXCLUDED.status, requested_by = EXCLUDED.requested_by, " +
            "requested_date = EXCLUDED.requested_date, purge_after = EXCLUDED.purge_after, " +
            "stripe_customer_id = NULL, stripe_subscription_id = NULL, purge_claimed_date = NULL, " +
            "teardown_failures = '[]' WHERE tenant_deletion.status = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, tenantId);
            preparedStatement.setInt(2, STATUS_PENDING_DELETION);
            preparedStatement.setString(3, requestedBy);
            preparedStatement.setObject(4, toLocalDateTime(Instant.now()));
            preparedStatement.setObject(5, toLocalDateTime(purgeAfter));
            preparedStatement.setInt(6, STATUS_CANCELLED);

            return preparedStatement.executeUpdate() == 1;
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public void markPurged(String tenantId) {
        String sql = "UPDATE public.tenant_deletion SET status = ? WHERE tenant_id = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, STATUS_PURGED);
            preparedStatement.setString(2, tenantId);

            preparedStatement.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public void recordStripeIds(String tenantId, String customerId, String subscriptionId) {
        String sql = "UPDATE public.tenant_deletion SET stripe_customer_id = ?, stripe_subscription_id = ? " +
            "WHERE tenant_id = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, customerId);
            preparedStatement.setString(2, subscriptionId);
            preparedStatement.setString(3, tenantId);

            preparedStatement.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    public void recordTeardownFailures(String tenantId, List<String> failures) {
        String sql = "UPDATE public.tenant_deletion SET teardown_failures = ? WHERE tenant_id = ?";

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, JsonUtils.write(failures));
            preparedStatement.setString(2, tenantId);

            preparedStatement.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage(), sqle);
        }
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static TenantDeletion toTenantDeletion(ResultSet resultSet) throws SQLException {
        String teardownFailures = resultSet.getString("teardown_failures");

        return new TenantDeletion(
            resultSet.getString("tenant_id"), Status.values()[resultSet.getInt("status")],
            resultSet.getString("requested_by"),
            toInstant(resultSet.getObject("requested_date", LocalDateTime.class)),
            toInstant(resultSet.getObject("purge_after", LocalDateTime.class)),
            resultSet.getString("stripe_customer_id"), resultSet.getString("stripe_subscription_id"),
            toInstant(resultSet.getObject("purge_claimed_date", LocalDateTime.class)),
            teardownFailures == null ? List.of() : JsonUtils.readList(teardownFailures, String.class));
    }
}
