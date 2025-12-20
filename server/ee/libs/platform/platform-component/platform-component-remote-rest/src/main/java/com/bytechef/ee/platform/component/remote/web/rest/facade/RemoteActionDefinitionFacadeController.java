/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.web.rest.facade;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
                propertiesRequest.lookupDependsOnPaths, propertiesRequest.workflowId, propertiesRequest.connectionId));
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
                performRequest.jobPrincipalId, performRequest.jobPrincipalWorkflowId, performRequest.jobId,
                performRequest.workflowId, performRequest.inputParameters, performRequest.connectionIds,
                performRequest.extensions, false, performRequest.type));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/execute-output",
        consumes = {
            "application/json"
        })
    public ResponseEntity<OutputResponse> executeOutput(
        @Valid @RequestBody RemoteActionDefinitionFacadeController.OutputRequest outputRequest) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeOutput(
                outputRequest.componentName, outputRequest.componentVersion, outputRequest.actionName,
                outputRequest.inputParameters, outputRequest.connectionIds));
    }

    @SuppressFBWarnings("EI")
    public record OptionsRequest(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> inputParameters, Long connectionId, List<String> lookupDependsOnPaths, String searchText) {
    }

    @SuppressFBWarnings("EI")
    public record OutputRequest(
        String componentName, int componentVersion, String actionName, Map<String, Object> inputParameters,
        Map<String, Long> connectionIds) {
    }

    @SuppressFBWarnings("EI")
    public record PropertiesRequest(
        String componentName, int componentVersion, String actionName, String propertyName, String workflowId,
        Map<String, Object> inputParameters, Long connectionId, List<String> lookupDependsOnPaths) {
    }

    @SuppressFBWarnings("EI")
    public record PerformRequest(
        String componentName, int componentVersion, String actionName, ModeType type, Long jobPrincipalId,
        Long jobPrincipalWorkflowId, long jobId, String workflowId, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds, Map<String, Long> extensions) {
    }
}
