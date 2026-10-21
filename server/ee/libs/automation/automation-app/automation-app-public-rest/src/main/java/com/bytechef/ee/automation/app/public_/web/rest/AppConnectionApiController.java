/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppActionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppActionModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppBindingModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.ExecuteActionRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The connection and action endpoints an app calls with its own secret key. {@link AppExecutionFacade} owns the
 * allow-list checks and the execution; this controller maps its results to the response shape.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppConnectionApiController extends AbstractAppApiController implements AppConnectionApi {

    private final AppExecutionFacade appExecutionFacade;

    @SuppressFBWarnings("EI2")
    public AppConnectionApiController(AppExecutionFacade appExecutionFacade) {
        this.appExecutionFacade = appExecutionFacade;
    }

    @Override
    public ResponseEntity<Object> executeAction(
        String bindingKey, String actionName, ExecuteActionRequestModel executeActionRequestModel) {

        Object result = appExecutionFacade.executeAction(
            bindingKey, actionName, executeActionRequestModel.getComponentVersion(),
            executeActionRequestModel.getInput());

        return ResponseEntity.ok(Collections.singletonMap("result", result));
    }

    @Override
    public ResponseEntity<List<AppActionModel>> getActions() {
        return ResponseEntity.ok(
            appExecutionFacade.getActions()
                .stream()
                .map(AppConnectionApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<List<AppBindingModel>> getConnections() {
        return ResponseEntity.ok(
            appExecutionFacade.getBindings()
                .stream()
                .map(AppConnectionApiController::toModel)
                .toList());
    }

    private static AppActionModel toModel(AppActionDTO action) {
        AppActionModel appActionModel = new AppActionModel(
            action.bindingKey(), action.componentName(), action.actionName(), action.inputSchema());

        appActionModel.setDescription(JsonNullable.of(action.description()));

        return appActionModel;
    }

    private static AppBindingModel toModel(AppBindingDTO binding) {
        AppBindingModel appBindingModel = new AppBindingModel(
            binding.bindingKey(), binding.componentName(), binding.connectionVersion(), binding.actionNames(),
            binding.toolNames(), binding.proxyEnabled(), binding.proxyAllowedMethods());

        appBindingModel.setComponentVersion(JsonNullable.of(binding.componentVersion()));

        return appBindingModel;
    }
}
