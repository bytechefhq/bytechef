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
 * @version ee
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
