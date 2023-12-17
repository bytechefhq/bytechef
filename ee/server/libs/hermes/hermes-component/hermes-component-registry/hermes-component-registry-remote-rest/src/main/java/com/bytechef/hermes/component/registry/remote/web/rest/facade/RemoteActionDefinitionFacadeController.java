/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.component.registry.remote.web.rest.facade;

import com.bytechef.hermes.component.registry.domain.EditorDescriptionResponse;
import com.bytechef.hermes.component.registry.domain.OptionsResponse;
import com.bytechef.hermes.component.registry.domain.OutputSchemaResponse;
import com.bytechef.hermes.component.registry.domain.PropertiesResponse;
import com.bytechef.hermes.component.registry.facade.ActionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
        value = "/execute-editor-description",
        consumes = {
            "application/json"
        })
    public ResponseEntity<EditorDescriptionResponse> executeEditorDescription(
        @Valid @RequestBody EditorDescriptionRequest editorDescriptionRequest) {

        return ResponseEntity.ok(actionDefinitionFacade.executeEditorDescription(
            editorDescriptionRequest.componentName, editorDescriptionRequest.componentVersion,
            editorDescriptionRequest.actionName, editorDescriptionRequest.inputParameters,
            editorDescriptionRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-options",
        consumes = {
            "application/json"
        })
    public ResponseEntity<OptionsResponse> executeOptions(@Valid @RequestBody OptionsRequest optionsRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOptions(
                optionsRequest.componentName, optionsRequest.componentVersion, optionsRequest.actionName,
                optionsRequest.propertyName, optionsRequest.inputParameters, optionsRequest.connectionId,
                optionsRequest.searchText));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-dynamic-properties",
        consumes = {
            "application/json"
        })
    public ResponseEntity<PropertiesResponse> executeDynamicProperties(
        @Valid @RequestBody PropertiesRequest propertiesRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeDynamicProperties(
                propertiesRequest.componentName, propertiesRequest.componentVersion, propertiesRequest.actionName,
                propertiesRequest.propertyName, propertiesRequest.inputParameters, propertiesRequest.connectionId));
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
                performRequest.type, performRequest.instanceId, performRequest.workflowId,
                performRequest.jobId, performRequest.inputParameters, performRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-output-schema",
        consumes = {
            "application/json"
        })
    public ResponseEntity<OutputSchemaResponse> executeOutputSchema(
        @Valid @RequestBody OutputSchemaRequest outputSchemaRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOutputSchema(
                outputSchemaRequest.componentName, outputSchemaRequest.componentVersion, outputSchemaRequest.actionName,
                outputSchemaRequest.inputParameters, outputSchemaRequest.connectionId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-sample-output",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Object> executeSampleOutput(@Valid @RequestBody SampleOutputRequest sampleOutputRequest) {
        return ResponseEntity.ok(
            actionDefinitionFacade.executeSampleOutput(
                sampleOutputRequest.componentName, sampleOutputRequest.componentVersion, sampleOutputRequest.actionName,
                sampleOutputRequest.inputParameters, sampleOutputRequest.connectionId));
    }

    @SuppressFBWarnings("EI")
    public record EditorDescriptionRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName, @NotNull String propertyName,
        Map<String, Object> inputParameters, Long connectionId, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputSchemaRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        @NotNull String propertyName, Map<String, Object> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record SampleOutputRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName,
        Map<String, Object> inputParameters, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record PerformRequest(
        @NotNull String componentName, int componentVersion, @NotNull String actionName, int type,
        Long instanceId, @NonNull String workflowId, long jobId, @NotNull Map<String, ?> inputParameters,
        Long connectionId) {
    }
}
