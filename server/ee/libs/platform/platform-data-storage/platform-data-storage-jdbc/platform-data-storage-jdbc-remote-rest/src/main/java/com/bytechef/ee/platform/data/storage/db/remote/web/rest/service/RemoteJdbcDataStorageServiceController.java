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
import com.bytechef.platform.data.storage.jdbc.domain.DataEntryVersion;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
        method = RequestMethod.DELETE,
        value = "/delete-scope-data/{scope}/{scopeId}/{type}")
    public ResponseEntity<Void> deleteScopeData(
        @PathVariable DataStorageScope scope, @PathVariable String scopeId, @PathVariable PlatformType type) {

        dataStorageService.deleteScopeData(scope, scopeId, type);

        return ResponseEntity.noContent()
            .build();
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

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-versioned/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}")
    public ResponseEntity<VersionedValue<Object>> fetchVersioned(
        @PathVariable String componentName, @PathVariable DataStorageScope scope,
        @PathVariable String scopeId, @PathVariable String key, @PathVariable Integer environment,
        @PathVariable PlatformType type) {

        Optional<VersionedValue<Object>> versionedValueOptional = dataStorageService.fetchVersioned(
            componentName, scope, scopeId, key, environment.intValue(), type);

        return versionedValueOptional.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/put-if-version/{componentName}/{scope}/{scopeId}/{key}/{environment}/{type}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Boolean> putIfVersion(
        @PathVariable String componentName, @PathVariable DataStorageScope scope,
        @PathVariable String scopeId, @PathVariable String key, @PathVariable Integer environment,
        @PathVariable PlatformType type, @RequestParam(required = false) Long expectedId,
        @RequestParam(required = false) Integer expectedVersion, @RequestBody Map<String, Object> body) {

        DataEntryVersion dataEntryVersion = expectedId == null || expectedVersion == null
            ? null : new DataEntryVersion(expectedId, expectedVersion);

        return ResponseEntity.ok(
            dataStorageService.putIfVersion(
                componentName, scope, scopeId, key, environment.intValue(), type, body.get("value"),
                dataEntryVersion));
    }
}
