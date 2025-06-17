/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.execution.facade.ToolFacade;
import com.bytechef.ee.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExecuteToolRequestModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.FunctionModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolModel;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class ToolApiController implements ToolApi {

    private final ToolFacade toolFacade;

    public ToolApiController(ToolFacade toolFacade) {
        this.toolFacade = toolFacade;
    }

    @Override
    public ResponseEntity<Object> executeTool(
        String externalUserId, EnvironmentModel xEnvironment, Long xInstanceId,
        ExecuteToolRequestModel executeToolRequestModel) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            toolFacade.executeTool(
                externalUserId, executeToolRequestModel.getName(), executeToolRequestModel.getParameters(),
                xInstanceId, environment));
    }

    @Override
    public ResponseEntity<Map<String, List<ToolModel>>> getTools(
        String externalUserId, EnvironmentModel xEnvironment, List<String> categories, List<String> components,
        List<String> tools) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            toolFacade
                .getTools(
                    externalUserId, categories == null ? List.of() : categories,
                    components == null ? List.of() : components, tools == null ? List.of() : tools, environment)
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        toolEntry -> toolEntry.getValue()
                            .stream()
                            .map(ToolApiController::toToolModel)
                            .toList())));
    }

    private static ToolModel toToolModel(ToolDTO toolDTO) {
        return new ToolModel()
            .function(
                new FunctionModel()
                    .name(toolDTO.name())
                    .description(toolDTO.description())
                    .parameters(toolDTO.parameters()));
    }
}
