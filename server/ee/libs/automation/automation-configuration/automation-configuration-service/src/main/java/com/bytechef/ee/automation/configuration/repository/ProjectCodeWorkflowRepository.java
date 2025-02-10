/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
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
public interface ProjectCodeWorkflowRepository extends ListCrudRepository<ProjectCodeWorkflow, Long> {
}
