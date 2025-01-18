/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.codeworkflow.facade;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowFacade {

    void save(long workspaceId, byte[] bytes, Language language);
}
