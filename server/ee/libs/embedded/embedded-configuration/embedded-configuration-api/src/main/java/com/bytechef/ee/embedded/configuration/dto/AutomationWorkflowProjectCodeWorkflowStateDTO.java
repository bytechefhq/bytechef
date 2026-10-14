/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AutomationWorkflowProjectCodeWorkflowStateDTO(Language language, boolean draftPublishable) {
}
