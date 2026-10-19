/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface EmbeddedUserRepository extends ListCrudRepository<EmbeddedUser, Long> {

    long countByCustomRoleId(long customRoleId);

    void deleteByUserId(long userId);

    boolean existsByUserId(long userId);

    boolean existsByUserIdAndEnvironmentIsNotNull(long userId);

    List<EmbeddedUser> findAllByUserId(long userId);

    Optional<EmbeddedUser> findByUserIdAndEnvironmentIsNull(long userId);

    Optional<EmbeddedUser> findByUserIdAndEnvironment(long userId, int environment);
}
