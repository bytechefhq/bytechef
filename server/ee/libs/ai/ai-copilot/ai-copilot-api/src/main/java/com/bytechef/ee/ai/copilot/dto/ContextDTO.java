/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ContextDTO(Source source, Map<String, ?> parameters, String workflowId) {

    public enum Source {
        WORKFLOW_EDITOR, WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU, CODE_EDITOR
    }
}
