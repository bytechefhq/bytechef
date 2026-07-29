/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.ee.embedded.ai.tool.model.IntegrationDetailInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Read-only subset of {@link IntegrationTools} for the ASK (read/plan) embedded workflow-editor agent. The embedded
 * mirror of the automation {@code ReadProjectTools}: it exposes only the non-mutating integration operations
 * (list/get/search) so the ASK agent can inspect integrations without being able to create, update, delete, or publish
 * them. Delegates to {@link IntegrationTools} to keep a single implementation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ReadIntegrationTools {

    private final IntegrationTools integrationTools;

    @SuppressFBWarnings("EI")
    public ReadIntegrationTools(IntegrationTools integrationTools) {
        this.integrationTools = integrationTools;
    }

    @Tool(
        description = "List all integrations in ByteChef. Returns a list of integrations with their basic information including id, name, description, wrapped component, and status.")
    public List<IntegrationInfo> listIntegrations() {
        return integrationTools.listIntegrations();
    }

    @Tool(
        description = "Get comprehensive information about a specific integration. Returns detailed information including id, name, description, wrapped component, status, and its workflow ids.")
    public IntegrationDetailInfo getIntegration(
        @ToolParam(description = "The ID of the integration to retrieve") long integrationId) {

        return integrationTools.getIntegration(integrationId);
    }

    @Tool(
        description = "Full-text search across all integrations. Returns a list of integrations matching the search query in name or description.")
    public List<IntegrationInfo> searchIntegrations(
        @ToolParam(description = "The search query to match against integration names and descriptions") String query) {

        return integrationTools.searchIntegrations(query);
    }
}
