/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade;
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
 * Builds the Context Store tool-callback lists shared by the Copilot source agents and the AI Hub
 * {@code context_store_agent} subagent. Read list feeds ASK; write list feeds BUILD.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreToolCallbacksFactory {

    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final ContextStoreQueryService contextStoreQueryService;
    private final WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;
    private final WorkspaceContextStoreFacade workspaceContextStoreFacade;
    private final @Nullable ContextStoreSemanticSearchService contextStoreSemanticSearchService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ContextStoreToolCallbacksFactory(
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService,
        ContextStoreQueryService contextStoreQueryService,
        WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade,
        WorkspaceContextStoreFacade workspaceContextStoreFacade,
        @Nullable ContextStoreSemanticSearchService contextStoreSemanticSearchService,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
        this.contextStoreQueryService = contextStoreQueryService;
        this.workspaceContextStoreSourceFacade = workspaceContextStoreSourceFacade;
        this.workspaceContextStoreFacade = workspaceContextStoreFacade;
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

        toolCallbacks.add(new CreateContextStoreSourceToolCallback(workspaceContextStoreSourceFacade));
        toolCallbacks.add(
            new UpdateContextStoreSourceToolCallback(
                workspaceContextStoreSourceFacade, workspaceContextStoreSourceService));
        toolCallbacks.add(
            new DeleteContextStoreSourceToolCallback(
                workspaceContextStoreSourceFacade, workspaceContextStoreSourceService));
        toolCallbacks.add(
            new RefreshContextStoreSourceToolCallback(
                workspaceContextStoreSourceFacade, workspaceContextStoreSourceService));
        toolCallbacks.add(
            new SetContextStoreSourceEnabledToolCallback(
                workspaceContextStoreSourceFacade, workspaceContextStoreSourceService));
        toolCallbacks.add(new DeleteContextStoreToolCallback(workspaceContextStoreFacade));

        return toolCallbacks;
    }
}
