/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

/**
 * {@link WorkspaceContextProvider} backed by a per-thread context snapshot. The calling copilot agent binds the active
 * {@link WorkspaceInvocationContext} via {@link #runWithContext(WorkspaceInvocationContext, Runnable)} immediately
 * before invoking Spring AI's {@code ChatClient}; the three workspace-file tool callbacks read those values during
 * execution.
 *
 * <p>
 * Uses an {@link InheritableThreadLocal} so child tasks forked from the binding thread (for example the Reactor
 * scheduler threads used by Spring AI streaming) inherit the parent thread's context when they are first initialised.
 * Tool callbacks additionally fall back to Spring AI's {@code ToolContext} when available, which is populated by
 * {@link WorkspaceInvocationContext#toToolContext()} to cover reactive hops that occur after the inheritable snapshot
 * was taken.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AgUiToolContextWorkspaceContextProvider implements WorkspaceContextProvider {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.workspaceFile.workspaceId";
    public static final String TOOL_CONTEXT_SOURCE_ORDINAL_KEY = "bytechef.workspaceFile.sourceOrdinal";
    public static final String TOOL_CONTEXT_LAST_USER_PROMPT_KEY = "bytechef.workspaceFile.lastUserPrompt";

    private static final InheritableThreadLocal<WorkspaceInvocationContext> HOLDER = new InheritableThreadLocal<>();

    /**
     * Binds the supplied invocation context to the current thread for the duration of the {@code runnable}, then
     * restores the previously bound value (if any) on the way out. Safe to nest.
     */
    public static void runWithContext(WorkspaceInvocationContext context, Runnable runnable) {
        WorkspaceInvocationContext previous = HOLDER.get();

        HOLDER.set(context);

        try {
            runnable.run();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }

    /**
     * Returns the context currently bound to this thread, or {@code null} when no copilot invocation is in flight.
     */
    public static WorkspaceInvocationContext currentContext() {
        return HOLDER.get();
    }

    @Override
    public Long currentWorkspaceId() {
        WorkspaceInvocationContext context = HOLDER.get();

        return context == null ? null : context.workspaceId();
    }

    @Override
    public Short currentSourceOrdinal() {
        WorkspaceInvocationContext context = HOLDER.get();

        return context == null ? null : context.sourceOrdinal();
    }

    @Override
    public String lastUserPrompt() {
        WorkspaceInvocationContext context = HOLDER.get();

        return context == null ? null : context.lastUserPrompt();
    }
}
