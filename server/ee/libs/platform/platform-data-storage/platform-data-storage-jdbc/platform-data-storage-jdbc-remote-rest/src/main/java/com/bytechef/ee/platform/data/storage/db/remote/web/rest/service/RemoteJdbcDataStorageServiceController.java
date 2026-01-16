/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.web.rest.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
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
@RequestMapping("/remote/jdbc-data-storage-service")
public class RemoteJdbcDataStorageServiceController {

    private final JdbcDataStorageService dataStorageService;

    @SuppressFBWarnings("EI")
    public RemoteJdbcDataStorageServiceController(JdbcDataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-value/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Object> fetchValue(
        @PathVariable String componentName, @PathVariable DataStorageScope scope,
        @PathVariable String scopeId, @PathVariable String key, @PathVariable Integer environment,
        @PathVariable PlatformType type) {

        return ResponseEntity.ok(
            OptionalUtils.orElse(
                dataStorageService.fetch(componentName, scope, scopeId, key, environment.intValue(), type), null));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/save/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> save(
        @PathVariable String componentName, @PathVariable DataStorageScope scope,
        @PathVariable String scopeId, @PathVariable String key, @PathVariable Integer environment,
        @PathVariable PlatformType type, @RequestBody Object data) {

        dataStorageService.put(componentName, scope, scopeId, key, environment.intValue(), type, data);

        return ResponseEntity.noContent()
            .build();
    }
}
