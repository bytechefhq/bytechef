/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppToolDTO;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppToolModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.ExecuteToolRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The tool endpoints an app calls with its own secret key. {@link AppExecutionFacade} owns the allow-list checks and
 * the execution; this controller maps its results to the response shape.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppToolApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppToolApiController extends AbstractAppApiController implements AppToolApi {

    private final AppExecutionFacade appExecutionFacade;

    @SuppressFBWarnings("EI2")
    public AppToolApiController(AppExecutionFacade appExecutionFacade) {
        this.appExecutionFacade = appExecutionFacade;
    }

    @Override
    public ResponseEntity<Object> executeTool(
        String bindingKey, String toolName, ExecuteToolRequestModel executeToolRequestModel) {

        Object result = appExecutionFacade.executeTool(bindingKey, toolName, executeToolRequestModel.getInput());

        return ResponseEntity.ok(Collections.singletonMap("result", result));
    }

    @Override
    public ResponseEntity<List<AppToolModel>> getTools() {
        return ResponseEntity.ok(
            appExecutionFacade.getTools()
                .stream()
                .map(AppToolApiController::toModel)
                .toList());
    }

    private static AppToolModel toModel(AppToolDTO appTool) {
        return new AppToolModel(
            appTool.name(), appTool.bindingKey(), appTool.componentName(), appTool.toolName(), appTool.parameters())
                .description(appTool.description());
    }
}
