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
public class CustomComponentTools {

    private static final Logger log = LoggerFactory.getLogger(CustomComponentTools.class);

    private final CustomComponentFacade customComponentFacade;

    @SuppressFBWarnings("EI")
    public CustomComponentTools(CustomComponentFacade customComponentFacade) {
        this.customComponentFacade = customComponentFacade;
    }

    @Tool(
        description = "Create a new empty custom component. Currently only the JAVASCRIPT language is supported. " +
            "Returns a confirmation message with the created custom component's ID.")
    public String createCustomComponent(
        @ToolParam(description = "The name of the custom component") String name,
        @ToolParam(
            description = "Language of the custom component, currently only JAVASCRIPT is supported") CustomComponent.Language language) {

        try {
            CustomComponent customComponent = customComponentFacade.createEmptyCustomComponent(name, language);

            if (log.isDebugEnabled()) {
                log.debug(
                    "createCustomComponent({}): Created custom component with id={}", name, customComponent.getId());
            }

            return "Created custom component " + customComponent.getId() + " (" + name + ").";
        } catch (Exception e) {
            log.error("createCustomComponent({}): Failed to create custom component", name, e);

            throw new ExecutionException(
                "Failed to create custom component: " + e.getMessage(), e, CustomComponentToolErrorType.CREATE);
        }
    }

    @Tool(
        description = "Update the source code of an existing custom component. Saving updates a DRAFT: editing a " +
            "PUBLISHED component spawns a new draft row (the source must bump .version() above the published one), " +
            "so the returned row id may differ from the requested id. Returns the resulting row's id, version and " +
            "status; use publishCustomComponent to make a draft live.")
    public String updateCustomComponentSource(
        @ToolParam(description = "The ID of the custom component to update") long id,
        @ToolParam(description = "The new source code content of the custom component") String content) {

        try {
            CustomComponent customComponent = customComponentFacade.updateCustomComponentSource(id, content);

            if (log.isDebugEnabled()) {
                log.debug("updateCustomComponentSource({}): Updated custom component source", id);
            }

            return "Updated custom component source; resulting row id " + customComponent.getId() + ", version " +
                customComponent.getComponentVersion() + ", status " + customComponent.getStatus() + ".";
        } catch (Exception e) {
            log.error("updateCustomComponentSource({}): Failed to update custom component source", id, e);

            throw new ExecutionException(
                "Failed to update custom component source: " + e.getMessage(), e,
                CustomComponentToolErrorType.UPDATE_SOURCE);
        }
    }

    @Tool(
        description = "Publish a DRAFT custom component so it becomes available to workflows. Only draft components " +
            "can be published; published versions are immutable. Returns a confirmation message.")
    public String publishCustomComponent(
        @ToolParam(description = "The ID of the draft custom component to publish") long id) {

        try {
            CustomComponent customComponent = customComponentFacade.publishCustomComponent(id);

            if (log.isDebugEnabled()) {
                log.debug("publishCustomComponent({}): Published custom component", id);
            }

            return "Published custom component " + customComponent.getId() + " (" + customComponent.getName() +
                ", version " + customComponent.getComponentVersion() + ").";
        } catch (Exception e) {
            log.error("publishCustomComponent({}): Failed to publish custom component", id, e);

            throw new ExecutionException(
                "Failed to publish custom component: " + e.getMessage(), e, CustomComponentToolErrorType.PUBLISH);
        }
    }

    @Tool(description = "Delete a custom component by its ID. Returns a confirmation message.")
    public String deleteCustomComponent(
        @ToolParam(description = "The ID of the custom component to delete") long id) {

        try {
            customComponentFacade.delete(id);

            if (log.isDebugEnabled()) {
                log.debug("deleteCustomComponent({}): Deleted custom component", id);
            }

            return "Deleted custom component " + id + ".";
        } catch (Exception e) {
            log.error("deleteCustomComponent({}): Failed to delete custom component", id, e);

            throw new ExecutionException(
                "Failed to delete custom component: " + e.getMessage(), e, CustomComponentToolErrorType.DELETE);
        }
    }
}
