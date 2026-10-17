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
 * The built-in set of AI Hub tool names that require approval with no workspace configuration at all: the destructive
 * project, workflow and deployment tools the hub registers. Beyond the explicitly listed names, a tool name whose first
 * word is a destructive verb ({@code delete}, {@code drop}, {@code rollback}, {@code promote}) is gated by prefix, so a
 * newly-added destructive catalog tool is safe by default without a code change here.
 *
 * <p>
 * Lives in {@code ai-hub-api}, not {@code ai-hub-service}, because {@link AiHubToolApprovalPolicy.Decision#isGated}
 * consults it directly and the interface it belongs to is itself an api type; {@code ai-hub-api} cannot depend on
 * {@code ai-hub-service}.
 * </p>
 *
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
