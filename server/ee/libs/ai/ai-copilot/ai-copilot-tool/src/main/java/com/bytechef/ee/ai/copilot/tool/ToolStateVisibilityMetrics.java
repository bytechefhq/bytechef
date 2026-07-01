/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

/**
 * Surface-neutral metric sink for shared tool callbacks: records a tool's state-visibility outcome (e.g. "success",
 * "empty", "connection_required").
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolStateVisibilityMetrics {

    ToolStateVisibilityMetrics NOOP = (toolName, state) -> {};

    void recordStateVisibility(String toolName, String state);

    /**
     * Records an {@code askUserQuestion} outcome to a dedicated metric where the surface tracks one;
     * default is a no-op for surfaces without a dedicated ask-user-question counter.
     */
    default void recordAskUserQuestion(String outcome) {
    }
}
