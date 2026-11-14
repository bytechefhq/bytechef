/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.repository;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public interface SigningKeyRepository extends ListCrudRepository<SigningKey, Long> {

    Optional<SigningKey> findByKeyIdAndEnvironment(String keyId, int environment);

    List<SigningKey> findAllByTypeAndEnvironment(int type, int environment);

    @Modifying
    @Query("""
        UPDATE signing_key SET last_used_date = :lastUsedDate
        WHERE key_id = :keyId AND environment = :environment
        AND (last_used_date IS NULL OR last_used_date < :staleBefore)
        """)
    int updateLastUsedDate(
        @Param("keyId") String keyId, @Param("environment") int environment,
        @Param("lastUsedDate") Instant lastUsedDate, @Param("staleBefore") Instant staleBefore);
}
