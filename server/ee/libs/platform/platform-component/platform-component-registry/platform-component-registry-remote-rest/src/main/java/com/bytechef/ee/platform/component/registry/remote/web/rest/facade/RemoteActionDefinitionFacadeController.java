/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.registry.remote.web.rest.facade;

import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/remote/action-definition-facade")
public class RemoteActionDefinitionFacadeController {

    private final ActionDefinitionFacade actionDefinitionFacade;

    public RemoteActionDefinitionFacadeController(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-dynamic-properties",
        consumes = {
            "application/json"
        })
    public ResponseEntity<List<Property>> executeDynamicProperties(
        @Valid @RequestBody PropertiesRequest propertiesRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeDynamicProperties(
                propertiesRequest.componentName, propertiesRequest.componentVersion, propertiesRequest.actionName,
                propertiesRequest.propertyName, propertiesRequest.inputParameters,
                propertiesRequest.lookupDependsOnPaths,
                propertiesRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-node-description",
        consumes = {
            "application/json"
        })
    public ResponseEntity<String> executeEditorDescription(
        @Valid @RequestBody NodeDescriptionRequest nodeDescriptionRequest) {

        return ResponseEntity.ok(actionDefinitionFacade.executeWorkflowNodeDescription(
            nodeDescriptionRequest.componentName, nodeDescriptionRequest.componentVersion,
            nodeDescriptionRequest.actionName, nodeDescriptionRequest.inputParameters));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-options",
        consumes = {
            "application/json"
        })
    public ResponseEntity<List<Option>> executeOptions(@Valid @RequestBody OptionsRequest optionsRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOptions(
                optionsRequest.componentName, optionsRequest.componentVersion, optionsRequest.actionName,
                optionsRequest.propertyName, optionsRequest.inputParameters, optionsRequest.lookupDependsOnPaths,
                optionsRequest.searchText, optionsRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-perform",
        produces = {
            "application/json"
        })
    public ResponseEntity<Object> executePerform(@Valid @RequestBody PerformRequest performRequest) {
        return ResponseEntity.ok(
            actionDefinitionFacade.executePerform(
                performRequest.componentName, performRequest.componentVersion, performRequest.actionName,
                performRequest.type, performRequest.instanceId, Long.getLong(performRequest.workflowId),
                performRequest.jobId, performRequest.inputParameters, performRequest.connectionIds));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-output",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Output> executeOutput(
        @Valid @RequestBody RemoteActionDefinitionFacadeController.OutputRequest outputRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOutput(
                outputRequest.componentName, outputRequest.componentVersion, outputRequest.actionName,
                outputRequest.inputParameters, outputRequest.connectionIds));
    }

    @SuppressFBWarnings("EI")
    public record NodeDescriptionRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> inputParameters) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName, @NotNull String propertyName,
        Map<String, Object> inputParameters, Long connectionId, List<String> lookupDependsOnPaths, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> inputParameters, @NotNull Map<String, Long> connectionIds) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        @NotNull String propertyName, Map<String, Object> inputParameters, Long connectionId,
        List<String> lookupDependsOnPaths) {
    }

    @SuppressFBWarnings("EI")
    public record PerformRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName, AppType type,
        Long instanceId, @NonNull String workflowId, long jobId, @NotNull Map<String, ?> inputParameters,
        @NotNull Map<String, Long> connectionIds) {
    }
}
