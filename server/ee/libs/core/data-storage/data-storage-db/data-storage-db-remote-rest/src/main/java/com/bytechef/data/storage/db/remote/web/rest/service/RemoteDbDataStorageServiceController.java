/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.data.storage.db.remote.web.rest.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.data.storage.db.service.DbDataStorageService;
import com.bytechef.platform.constant.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping("/remote/db-data-storage-service")
public class RemoteDbDataStorageServiceController {

    private final DbDataStorageService dataStorageService;

    @SuppressFBWarnings("EI")
    public RemoteDbDataStorageServiceController(DbDataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-value/{componentName}/{actionName}/{scope}/{scopeId}/{key}/{type}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Object> fetchValue(
        @PathVariable String componentName, @PathVariable String actionName, @PathVariable Scope scope,
        @PathVariable String scopeId, @PathVariable String key, @PathVariable Type type) {

        return ResponseEntity.ok(
            OptionalUtils.orElse(dataStorageService.fetch(
                componentName, actionName, scope, scopeId, key, type), null));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/save/{componentName}/{actionName}/{scope}/{scopeId}/{key}/{type}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> save(
        @PathVariable String componentName, @PathVariable String actionName, @PathVariable Scope scope,
        @PathVariable String scopeId, @PathVariable String key, @PathVariable Type type, @RequestBody Object data) {

        dataStorageService.put(componentName, actionName, scope, scopeId, key, type, data);

        return ResponseEntity.noContent()
            .build();
    }
}
