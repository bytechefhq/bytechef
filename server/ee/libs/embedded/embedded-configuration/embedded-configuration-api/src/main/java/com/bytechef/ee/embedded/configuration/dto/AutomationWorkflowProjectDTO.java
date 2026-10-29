/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectDTO(
    long id, String name, String description, Long categoryId, List<Long> tagIds, boolean published, int version,
    Integer lastPublishedVersion, List<ConnectedUserWorkflowTemplateDTO> workflowTemplates,
    String permissionExpression, boolean automationHubVisible, int configurationCount,
    @Nullable Instant lastPublishedDate) {
}
