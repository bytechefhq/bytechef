/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import java.util.Map;

/**
 * The connected user's own switch for making an automation workflow available to AI agents, across every MCP server
 * that offers it to the user. It is independent of the on and off state of the automation workflow itself.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpAutomationWorkflowFacade {

    /**
     * Turns the MCP rows of the connected user's reference for the workflow uuid on or off, on every server.
     *
     * @param connectedUserId the connected user id
     * @param workflowUuid    the automation workflow uuid
     * @param enable          whether to turn the rows on
     */
    void enableMcpAutomationWorkflow(long connectedUserId, String workflowUuid, boolean enable);

    /**
     * Returns, per automation workflow uuid, whether the workflow is available to AI agents. A uuid is present only
     * when at least one MCP server offers the workflow to the user, and then its value is true when any of the user's
     * rows for it is on.
     *
     * @param connectedUserId the connected user id
     * @return the availability keyed by automation workflow uuid
     */
    Map<String, Boolean> getMcpEnabledByAutomationUuid(long connectedUserId);
}
