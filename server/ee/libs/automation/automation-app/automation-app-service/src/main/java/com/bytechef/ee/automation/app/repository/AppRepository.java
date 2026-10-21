/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.App;
import java.time.Instant;
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
public interface AppRepository extends ListCrudRepository<App, Long> {

    List<App> findAllByWorkspaceIdAndEnvironmentOrderByName(long workspaceId, int environment);

    Optional<App> findBySecretKeyHash(String secretKeyHash);

    Optional<App> findByUuid(String uuid);

    Optional<App> findByWorkspaceIdAndEnvironmentAndName(long workspaceId, int environment, String name);

    /**
     * Writes the last-used mark without touching {@code version} or the audit columns, and only when the stored value
     * is older than {@code cutoff}, so a busy app does not write on every request and never races an admin's
     * optimistic-locked save.
     */
    @Modifying
    @Query("""
        UPDATE app SET secret_key_last_used_date = :now
        WHERE id = :id
            AND (secret_key_last_used_date IS NULL OR secret_key_last_used_date < CAST(:cutoff AS TIMESTAMP))
        """)
    int touchSecretKeyLastUsedDate(@Param("id") long id, @Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
