/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.task.dispatcher.registry.remote.web.rest.service;

import com.bytechef.hermes.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/task-dispatcher-definition-service")
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "coordinator-app")
public class RemoteTaskDispatcherDefinitionServiceController {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public RemoteTaskDispatcherDefinitionServiceController(
        TaskDispatcherDefinitionService taskDispatcherDefinitionService) {

        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-task-dispatcher-definition/{name}/{version}",
        produces = {
            "application/json"
        })
    public ResponseEntity<TaskDispatcherDefinition> getTaskDispatcherDefinition(
        @PathVariable("name") String name, @PathVariable("version") Integer version) {

        return ResponseEntity.ok(taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-task-dispatcher-definitions",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TaskDispatcherDefinition>> getTaskDispatcherDefinitions() {
        return ResponseEntity.ok(taskDispatcherDefinitionService.getTaskDispatcherDefinitions());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-task-dispatcher-definition-versions/{name}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TaskDispatcherDefinition>> getTaskDispatcherDefinitionVersions(
        @PathVariable("name") String name) {

        return ResponseEntity.ok(taskDispatcherDefinitionService.getTaskDispatcherDefinitionVersions(name));
    }
}
