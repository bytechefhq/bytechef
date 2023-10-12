/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.service;

import com.bytechef.athena.configuration.domain.Integration;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationService {

    Integration addWorkflow(long id, String workflowId);

    Integration create(Integration integration);

    void delete(long id);

    Integration getIntegration(long id);

    List<Integration> getIntegrations(Long categoryId, Long tagId);

    void removeWorkflow(long id, String workflowId);

    Integration update(long id, List<Long> tagIds);

    Integration update(Integration integration);

}
