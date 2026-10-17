/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.util.Set;

/**
 * Decides, for one chat turn, which tool names require a person's approval before they execute. Combines the built-in
 * defaults ({@link AiHubToolApprovalDefaults}) and the workspace's own CATALOG {@link AiHubToolApprovalRule} rows into
 * a single {@link Decision}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubToolApprovalPolicy {

    Decision decide(long workspaceId);

    /**
     * {@code required} and {@code exempt} are evaluated together: an exempt tool is never gated, even when it also
     * matches a built-in default prefix or an explicit {@code required} entry. A workspace {@code EXEMPT} rule is the
     * escape hatch for a default that doesn't fit a particular workspace.
     */
    record Decision(Set<String> required, Set<String> exempt) {

        /**
         * Both sets are copied, not stored as handed in. {@code isGated} reads them on every gate check for the whole
         * turn, so a caller that kept its own reference to the mutable set it passed could add a tool to {@code exempt}
         * (or drop one from {@code required}) and silently switch an approval gate off after the decision was made.
         */
        public Decision {
            required = Set.copyOf(required);
            exempt = Set.copyOf(exempt);
        }

        public boolean isGated(String toolName) {
            if (exempt.contains(toolName)) {
                return false;
            }

            return required.contains(toolName) || AiHubToolApprovalDefaults.isGatedByDefault(toolName);
        }
    }
}
