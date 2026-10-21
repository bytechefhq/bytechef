/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.repository;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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

    @Modifying
    @Query("DELETE FROM signing_key WHERE user_id = :userId")
    void deleteAllByUserId(@Param("userId") long userId);

    Optional<SigningKey> findByKeyIdAndEnvironment(String keyId, int environment);

    List<SigningKey> findAllByTypeAndEnvironment(int type, int environment);
}
