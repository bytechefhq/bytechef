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
 * This factory is additive: {@link ApiCollectionManagerConfiguration}'s existing {@code apiCollectionManagerChatClient}
 * bean keeps constructing its own tool list independently, since it backs the {@code api_collection_manager} subagent
 * consumed by AI Hub and the management MCP server and must not change behaviour.
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
        toolCallbacks.add(new CloneApiCollectionToolCallback(apiCollectionFacade));

        return toolCallbacks;
    }
}
