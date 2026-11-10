/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import java.util.List;
import java.util.Set;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AutomationProjectConfigurationRepository
    extends ListCrudRepository<AutomationProjectConfiguration, Long> {

    List<AutomationProjectConfiguration> findAllByProjectIdOrderByIdAsc(long projectId);

    List<AutomationProjectConfiguration> findAllByProjectIdInOrderByIdAsc(Set<Long> projectIds);

    List<AutomationProjectConfiguration> findAllByProjectIdAndEnvironmentIdOrderByIdAsc(
        long projectId, long environmentId);
}
