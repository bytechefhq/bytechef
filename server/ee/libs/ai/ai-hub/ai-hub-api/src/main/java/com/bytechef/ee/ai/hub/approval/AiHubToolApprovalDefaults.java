/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubToolApprovalDefaults {

    public static final Set<String> DEFAULT_TOOL_NAMES = Set.of(
        "deleteProject", "deleteWorkflow", "deleteProjectDeployment", "rollbackProjectDeployment", "promoteWorkflow");

    private static final List<String> GATED_PREFIXES = List.of("delete", "drop", "rollback", "promote");

    private AiHubToolApprovalDefaults() {
    }

    public static boolean isGatedByDefault(String toolName) {
        if (DEFAULT_TOOL_NAMES.contains(toolName)) {
            return true;
        }

        for (String prefix : GATED_PREFIXES) {
            if (toolName.startsWith(prefix) && toolName.length() > prefix.length()
                && Character.isUpperCase(toolName.charAt(prefix.length()))) {

                return true;
            }
        }

        return false;
    }
}
