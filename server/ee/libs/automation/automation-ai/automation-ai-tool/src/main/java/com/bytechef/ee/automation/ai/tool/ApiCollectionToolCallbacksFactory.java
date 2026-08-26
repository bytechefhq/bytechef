/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the API Collection tool-callback lists shared by the Copilot panel agents ({@code api_collection_ask} /
 * {@code api_collection_build}). Read list feeds ASK; write list feeds BUILD.
 *
 * <p>
 * This factory is now also the sole source for the AI Hub ASK/BUILD agents (via
 * {@code AiHubConfiguration#apiCollectionFlatCrudToolCallbacks}) and the management MCP server (via
 * {@code ApiCollectionAgentConfiguration#apiCollectionFlatCrudMcpContributor}). The former {@code api_collection_agent}
 * delegate (dissolved ticket 732, Task 2 of the CRUD-delegate unwind), which used to construct its own independent tool
 * list wrapping the same three {@link ToolCallback} classes, is gone — this factory is no longer merely additive to it.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiCollectionToolCallbacksFactory {

    private final ApiCollectionFacade apiCollectionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ApiCollectionToolCallbacksFactory(ApiCollectionFacade apiCollectionFacade) {
        this.apiCollectionFacade = apiCollectionFacade;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListApiCollectionsToolCallback(apiCollectionFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateApiCollectionToolCallback(apiCollectionFacade));

        return toolCallbacks;
    }
}
