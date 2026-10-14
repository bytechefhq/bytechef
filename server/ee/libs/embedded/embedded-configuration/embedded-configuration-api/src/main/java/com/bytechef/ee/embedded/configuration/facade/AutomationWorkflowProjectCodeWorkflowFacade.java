/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowSourceDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowStateDTO;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectCodeWorkflowFacade {

    long createEmptyCodeWorkflow(
        String name, Language language, @Nullable String description, @Nullable String category,
        @Nullable List<String> tags, @Nullable String permissionExpression);

    AutomationWorkflowProjectCodeWorkflowSourceDTO getCodeWorkflowSource(long automationWorkflowProjectId);

    Optional<AutomationWorkflowProjectCodeWorkflowStateDTO> getCodeWorkflowState(long automationWorkflowProjectId);

    List<String> save(byte[] bytes, Language language);

    void updateCodeWorkflowSource(long automationWorkflowProjectId, String content);
}
