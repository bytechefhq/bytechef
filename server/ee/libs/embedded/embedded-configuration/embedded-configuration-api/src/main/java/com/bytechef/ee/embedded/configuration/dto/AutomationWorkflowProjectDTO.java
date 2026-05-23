/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectDTO(
    long id, String name, String description, Long categoryId, List<Long> tagIds, boolean published, int version,
    Integer lastPublishedVersion, List<ConnectedUserWorkflowTemplateDTO> workflowTemplates) {
}
