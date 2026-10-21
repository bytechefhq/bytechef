/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppInstance;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AppInstanceRepository extends ListCrudRepository<AppInstance, Long> {

    List<AppInstance> findAllByAppIdOrderById(long appId);

    @Query("SELECT * FROM app_instance WHERE app_id IN (:appIds) ORDER BY id")
    List<AppInstance> findAllByAppIdInOrderById(@Param("appIds") Collection<Long> appIds);

    List<AppInstance> findAllByDeploymentConnectionIdOrderById(long deploymentConnectionId);

    List<AppInstance> findAllByWorkspaceIdAndEnvironmentOrderById(long workspaceId, int environment);

    Optional<AppInstance> findByAppIdAndEnvironmentAndName(long appId, int environment, String name);

    Optional<AppInstance> findBySecretKeyHash(String secretKeyHash);

    Optional<AppInstance> findByUuid(String uuid);

    @Query("SELECT id FROM app_instance WHERE id = :appInstanceId FOR UPDATE")
    Optional<Long> lockAppInstance(@Param("appInstanceId") long appInstanceId);

    /**
     * Writes the last-used mark without touching {@code version} or the audit columns, and only when the stored value
     * is older than {@code cutoff}, so a busy instance does not write on every request and never races an admin's
     * optimistic-locked save.
     */
    @Modifying
    @Query("""
        UPDATE app_instance SET secret_key_last_used_date = :now
        WHERE id = :id
            AND (secret_key_last_used_date IS NULL OR secret_key_last_used_date < CAST(:cutoff AS TIMESTAMP))
        """)
    int touchSecretKeyLastUsedDate(@Param("id") long id, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
