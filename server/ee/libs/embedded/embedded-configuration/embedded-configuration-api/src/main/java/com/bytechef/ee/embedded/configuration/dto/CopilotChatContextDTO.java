/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI", "EI2"
})
public record CopilotChatContextDTO(String workflowId, Set<String> allowedComponentNames) {
}
