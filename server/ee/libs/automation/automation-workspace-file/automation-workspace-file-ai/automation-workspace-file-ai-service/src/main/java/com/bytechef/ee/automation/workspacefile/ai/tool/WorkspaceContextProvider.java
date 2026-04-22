/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workspacefile.ai.tool;

/**
 * Supplies the workspace, agent-source, and originating-prompt context used when the copilot creates or reads workspace
 * files on behalf of a user. The real implementation is wired up by the calling copilot agent in
 * {@code ai-copilot-app}.
 *
 * <p>
 * The tool callbacks use this interface to resolve the active request's workspace context without requiring direct
 * coupling to Spring AI's {@code ToolContext} plumbing. Implementations are expected to be thread-scoped so each
 * request reads its own values; unit tests can substitute a mock without wiring any infrastructure.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceContextProvider {

    /**
     * Returns the id of the workspace the active copilot request is operating on. May be {@code null} when the request
     * originates from an agent that does not carry workspace context (e.g. the workflow-editor copilot).
     */
    Long currentWorkspaceId();

    /**
     * Returns the ordinal of the {@code Source} enum that issued the request (e.g. FILES, WORKFLOW_EDITOR) so saved
     * files can be attributed back to the originating agent. May be {@code null} when no source is available.
     */
    Short currentSourceOrdinal();

    /**
     * Returns the most recent user prompt that triggered the current tool call, used to annotate AI-generated files.
     * May be {@code null} when no user prompt is associated with the invocation.
     */
    String lastUserPrompt();
}
