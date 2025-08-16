/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.WorkspaceUser;
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
public interface WorkspaceUserRepository extends ListCrudRepository<WorkspaceUser, Long> {

    List<WorkspaceUser> findAllByUserId(long userId);

    List<WorkspaceUser> findAllByWorkspaceId(long workspaceId);

    Optional<WorkspaceUser> findByUserId(long userId);
}
