/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface CustomRoleRepository extends ListCrudRepository<CustomRole, Long> {

    /**
     * Whether any role already holds this name. Exists so the write paths can reject a duplicate themselves:
     * {@code uk_custom_role_name} would otherwise raise a {@code DuplicateKeyException} that cannot be recovered from,
     * because PostgreSQL marks the transaction aborted and the commit fails whatever the handler does.
     */
    boolean existsByName(String name);
}
