/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the Context Store tool-callback lists shared by the Copilot source agents, the AI Hub's flat/catalog
 * context-store registrations (ticket 732, CRUD-delegate-unwind Task 7 — see
 * {@code AiHubConfiguration#contextStoreFlatCrudToolCallbacks}/{@code #contextStoreCatalogToolCallbacks}), and the
 * management MCP server's flat registration (see {@code AutomationCopilotMcpContributorConfiguration
 * #contextStoreFlatCrudMcpContributor}). Read list feeds ASK; write list feeds BUILD.
 *
 * <p>
 * The five source mutations are built over the authorization-enforcing {@link ContextStoreSourceFacade} (admin role
 * required), not the deliberately unguarded {@code WorkspaceContextStoreSourceFacade} the reads and the data plane use
 * — their LLM-visible descriptions state that requirement, so it has to hold on every surface they reach. The top-level
 * store delete is built the same way, over the authorization-enforcing {@link ContextStoreFacade} rather than the
 * unguarded {@code WorkspaceContextStoreFacade}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreToolCallbacksFactory {

    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final ContextStoreQueryService contextStoreQueryService;
    private final ContextStoreSourceFacade contextStoreSourceFacade;
    private final ContextStoreFacade contextStoreFacade;
    private final @Nullable ContextStoreSemanticSearchService contextStoreSemanticSearchService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ContextStoreToolCallbacksFactory(
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService,
        ContextStoreQueryService contextStoreQueryService,
        ContextStoreSourceFacade contextStoreSourceFacade,
        ContextStoreFacade contextStoreFacade,
        @Nullable ContextStoreSemanticSearchService contextStoreSemanticSearchService,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
        this.contextStoreQueryService = contextStoreQueryService;
        this.contextStoreSourceFacade = contextStoreSourceFacade;
        this.contextStoreFacade = contextStoreFacade;
        this.contextStoreSemanticSearchService = contextStoreSemanticSearchService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListContextSourcesToolCallback(workspaceContextStoreSourceService));
        toolCallbacks.add(
            new SearchContextStoreToolCallback(contextStoreQueryService, workspaceContextStoreSourceService));
        toolCallbacks.add(
            new GetContextStoreRecordToolCallback(contextStoreQueryService, workspaceContextStoreSourceService));
        toolCallbacks.add(new ListAvailableSourceComponentsToolCallback());
        toolCallbacks.add(new DescribeSourceComponentEntitiesToolCallback(clusterElementDefinitionService));

        if (contextStoreSemanticSearchService != null) {
            toolCallbacks.add(
                new SemanticSearchContextStoreToolCallback(
                    contextStoreSemanticSearchService, workspaceContextStoreSourceService));
        }

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateContextStoreSourceToolCallback(contextStoreSourceFacade));
        toolCallbacks.add(new UpdateContextStoreSourceToolCallback(contextStoreSourceFacade));
        toolCallbacks.add(new DeleteContextStoreSourceToolCallback(contextStoreSourceFacade));
        toolCallbacks.add(new RefreshContextStoreSourceToolCallback(contextStoreSourceFacade));
        toolCallbacks.add(new SetContextStoreSourceEnabledToolCallback(contextStoreSourceFacade));
        toolCallbacks.add(new DeleteContextStoreToolCallback(contextStoreFacade));

        return toolCallbacks;
    }
}
