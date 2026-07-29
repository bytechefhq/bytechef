/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.ee.embedded.ai.tool.exception.IntegrationToolErrorType;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationDetailInfo;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationInfo;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for managing embedded integrations. The embedded mirror of the automation {@code ProjectTools},
 * re-keyed from projects onto the component-backed integration model: unlike a project, an integration wraps a single
 * component ({@code componentName}/{@code componentVersion}), so creation requires a component reference.
 *
 * <p>
 * The tenant-admin authorization is enforced at the {@link IntegrationFacade} layer, matching the rest of the embedded
 * management surface; these tools carry no authorization of their own.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class IntegrationTools {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTools.class);

    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI")
    public IntegrationTools(IntegrationFacade integrationFacade) {
        this.integrationFacade = integrationFacade;
    }

    @Tool(
        description = "List all integrations in ByteChef. Returns a list of integrations with their basic information including id, name, description, wrapped component, and status.")
    public List<IntegrationInfo> listIntegrations() {
        try {
            List<IntegrationInfo> integrationInfos = integrationFacade.getIntegrations(null, false, null, null, false)
                .stream()
                .map(IntegrationTools::getIntegrationInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug("listIntegrations(): Found {} integrations", integrationInfos.size());
            }

            return integrationInfos;
        } catch (Exception exception) {
            log.error("listIntegrations(): Failed to list integrations", exception);

            throw new ExecutionException(
                "Failed to list integrations: " + exception.getMessage(), exception,
                IntegrationToolErrorType.LIST_INTEGRATIONS);
        }
    }

    @Tool(
        description = "Get comprehensive information about a specific integration. Returns detailed information including id, name, description, wrapped component, status, and its workflow ids.")
    public IntegrationDetailInfo getIntegration(
        @ToolParam(description = "The ID of the integration to retrieve") long integrationId) {

        try {
            IntegrationDTO integrationDTO = integrationFacade.getIntegration(integrationId);

            if (log.isDebugEnabled()) {
                log.debug("getIntegration({}): Retrieved integration", integrationId);
            }

            return getIntegrationDetailInfo(integrationDTO);
        } catch (Exception exception) {
            log.error("getIntegration({}): Failed to get integration", integrationId, exception);

            throw new ExecutionException(
                "Failed to get integration: " + exception.getMessage(), exception,
                IntegrationToolErrorType.GET_INTEGRATION);
        }
    }

    @Tool(
        description = "Full-text search across all integrations. Returns a list of integrations matching the search query in name or description.")
    public List<IntegrationInfo> searchIntegrations(
        @ToolParam(description = "The search query to match against integration names and descriptions") String query) {

        try {
            String lowerQuery = query.trim()
                .toLowerCase();

            List<IntegrationInfo> matchingIntegrations = integrationFacade
                .getIntegrations(null, false, null, null, false)
                .stream()
                .filter(integrationDTO -> {
                    String name = StringUtils.defaultString(integrationDTO.name())
                        .toLowerCase();
                    String description = StringUtils.defaultString(integrationDTO.description())
                        .toLowerCase();

                    return name.contains(lowerQuery) || description.contains(lowerQuery);
                })
                .map(IntegrationTools::getIntegrationInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "searchIntegrations({}): Found {} integrations matching query", query, matchingIntegrations.size());
            }

            return matchingIntegrations;
        } catch (Exception exception) {
            log.error("searchIntegrations({}): Failed to search integrations", query, exception);

            throw new ExecutionException(
                "Failed to search integrations: " + exception.getMessage(), exception,
                IntegrationToolErrorType.SEARCH_INTEGRATIONS);
        }
    }

    @Tool(
        description = "Create a new integration that wraps a component. Returns the created integration information. An integration is bound to a single component identified by componentName/componentVersion.")
    public IntegrationInfo createIntegration(
        @ToolParam(description = "The name of the component the integration wraps (e.g. 'slack')") String componentName,
        @ToolParam(description = "The version of the component the integration wraps") int componentVersion,
        @ToolParam(required = false, description = "The name of the new integration") String name,
        @ToolParam(required = false, description = "The description of the new integration") String description,
        @ToolParam(
            required = false,
            description = "Whether the integration allows multiple connected instances (defaults to false)") Boolean multipleInstances) {

        try {
            IntegrationDTO integrationDTO = new IntegrationDTO(
                null, false, null, componentName, componentVersion, null, null, StringUtils.trimToNull(description),
                null, null, List.of(), List.of(), null, null, null, null, null,
                multipleInstances != null && multipleInstances, StringUtils.trimToNull(name), null, List.of(), null, 0);

            long integrationId = integrationFacade.createIntegration(integrationDTO);

            IntegrationDTO createdIntegrationDTO = integrationFacade.getIntegration(integrationId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "createIntegration({}, {}, {}): Created integration {}", componentName, componentVersion, name,
                    integrationId);
            }

            return getIntegrationInfo(createdIntegrationDTO);
        } catch (Exception exception) {
            log.error(
                "createIntegration({}, {}, {}): Failed to create integration", componentName, componentVersion, name,
                exception);

            throw new ExecutionException(
                "Failed to create integration: " + exception.getMessage(), exception,
                IntegrationToolErrorType.CREATE_INTEGRATION);
        }
    }

    @Tool(
        description = "Update an integration's name and/or description. Returns the updated integration information.")
    public IntegrationInfo updateIntegration(
        @ToolParam(description = "The ID of the integration to update") long integrationId,
        @ToolParam(required = false, description = "The new name of the integration") String name,
        @ToolParam(required = false, description = "The new description of the integration") String description) {

        try {
            IntegrationDTO existing = integrationFacade.getIntegration(integrationId);

            String newName = StringUtils.isBlank(name) ? existing.name() : name.trim();
            String newDescription = description != null ? description : existing.description();

            IntegrationDTO integrationDTO = new IntegrationDTO(
                existing.category(), existing.codeWorkflow(), existing.codeWorkflowLanguage(), existing.componentName(),
                existing.componentVersion(), existing.createdBy(), existing.createdDate(), newDescription,
                existing.icon(), existing.id(), existing.integrationVersions(), existing.integrationWorkflowIds(),
                existing.lastModifiedBy(), existing.lastModifiedDate(), existing.lastPublishedDate(),
                existing.lastStatus(), existing.lastIntegrationVersion(), existing.multipleInstances(), newName,
                existing.permissionExpression(), existing.tags(), existing.title(), existing.version());

            integrationFacade.updateIntegration(integrationDTO);

            IntegrationDTO updatedIntegrationDTO = integrationFacade.getIntegration(integrationId);

            if (log.isDebugEnabled()) {
                log.debug("updateIntegration({}, {}): Updated integration", integrationId, name);
            }

            return getIntegrationInfo(updatedIntegrationDTO);
        } catch (Exception exception) {
            log.error("updateIntegration({}, {}): Failed to update integration", integrationId, name, exception);

            throw new ExecutionException(
                "Failed to update integration: " + exception.getMessage(), exception,
                IntegrationToolErrorType.UPDATE_INTEGRATION);
        }
    }

    @Tool(
        description = "Delete an integration and all its workflows. Returns a confirmation message.")
    public String deleteIntegration(
        @ToolParam(description = "The ID of the integration to delete") long integrationId) {

        try {
            IntegrationDTO integrationDTO = integrationFacade.getIntegration(integrationId);

            String name = integrationDTO.name();

            integrationFacade.deleteIntegration(integrationId);

            if (log.isDebugEnabled()) {
                log.debug("deleteIntegration({}): Deleted integration", integrationId);
            }

            return "Integration '" + name + "' (ID: " + integrationId +
                ") has been successfully deleted along with all its workflows.";
        } catch (Exception exception) {
            log.error("deleteIntegration({}): Failed to delete integration", integrationId, exception);

            throw new ExecutionException(
                "Failed to delete integration: " + exception.getMessage(), exception,
                IntegrationToolErrorType.DELETE_INTEGRATION);
        }
    }

    @Tool(
        description = "Publish an integration version for deployment. Returns a confirmation message.")
    public String publishIntegration(
        @ToolParam(description = "The ID of the integration to publish") long integrationId,
        @ToolParam(required = false, description = "The description for this published version") String description) {

        try {
            integrationFacade.publishIntegration(integrationId, StringUtils.trimToNull(description));

            if (log.isDebugEnabled()) {
                log.debug("publishIntegration({}, {}): Published integration", integrationId, description);
            }

            return "Integration (ID: " + integrationId + ") has been successfully published.";
        } catch (Exception exception) {
            log.error("publishIntegration({}, {}): Failed to publish integration", integrationId, description,
                exception);

            throw new ExecutionException(
                "Failed to publish integration: " + exception.getMessage(), exception,
                IntegrationToolErrorType.PUBLISH_INTEGRATION);
        }
    }

    private static IntegrationInfo getIntegrationInfo(IntegrationDTO integrationDTO) {
        return new IntegrationInfo(
            integrationDTO.id(), integrationDTO.name(), integrationDTO.description(), integrationDTO.componentName(),
            resolveStatus(integrationDTO), integrationDTO.createdDate(), integrationDTO.lastModifiedDate());
    }

    private static IntegrationDetailInfo getIntegrationDetailInfo(IntegrationDTO integrationDTO) {
        return new IntegrationDetailInfo(
            integrationDTO.id(), integrationDTO.name(), integrationDTO.description(), integrationDTO.componentName(),
            integrationDTO.componentVersion(), resolveStatus(integrationDTO), integrationDTO.multipleInstances(),
            integrationDTO.integrationWorkflowIds(), integrationDTO.createdDate(), integrationDTO.lastModifiedDate(),
            integrationDTO.lastPublishedDate());
    }

    private static String resolveStatus(IntegrationDTO integrationDTO) {
        Status status = integrationDTO.lastStatus();

        if (status != null) {
            return status.name();
        }

        return integrationDTO.lastPublishedDate() == null ? Status.DRAFT.name() : Status.PUBLISHED.name();
    }
}
