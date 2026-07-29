/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ee.automation.ai.tool.exception.CustomComponentToolErrorType;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ReadCustomComponentTools {

    private static final Logger log = LoggerFactory.getLogger(ReadCustomComponentTools.class);

    private final CustomComponentFacade customComponentFacade;

    @SuppressFBWarnings("EI")
    public ReadCustomComponentTools(CustomComponentFacade customComponentFacade) {
        this.customComponentFacade = customComponentFacade;
    }

    @Tool(description = "Read the source code of a custom component by its ID. Returns the source code as text.")
    public String getCustomComponentSource(
        @ToolParam(description = "The ID of the custom component") long id) {

        try {
            String source = customComponentFacade.getCustomComponentSource(id);

            if (log.isDebugEnabled()) {
                log.debug("getCustomComponentSource({}): Retrieved custom component source", id);
            }

            return source;
        } catch (Exception e) {
            log.error("getCustomComponentSource({}): Failed to get custom component source", id, e);

            throw new ExecutionException(
                "Failed to get custom component source: " + e.getMessage(), e,
                CustomComponentToolErrorType.GET_SOURCE);
        }
    }

    @Tool(description = "List all available custom components. Returns a list of custom component metadata.")
    public List<CustomComponent> listCustomComponents() {
        try {
            List<CustomComponent> customComponents = customComponentFacade.getCustomComponents();

            if (log.isDebugEnabled()) {
                log.debug("listCustomComponents(): Found {} custom component(s)", customComponents.size());
            }

            return customComponents;
        } catch (Exception e) {
            log.error("listCustomComponents(): Failed to list custom components", e);

            throw new ExecutionException(
                "Failed to list custom components: " + e.getMessage(), e, CustomComponentToolErrorType.LIST);
        }
    }
}
