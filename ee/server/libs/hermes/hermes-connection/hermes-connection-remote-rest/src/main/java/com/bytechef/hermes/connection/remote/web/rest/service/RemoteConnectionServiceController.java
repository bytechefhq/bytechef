/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.connection.remote.web.rest.service;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
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
@RequestMapping("/remote/connection-service")
public class RemoteConnectionServiceController {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public RemoteConnectionServiceController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-connection/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Connection> fetchConnection(@PathVariable long id) {
        return connectionService.fetchConnection(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-connections",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Connection>> getConnections() {
        return ResponseEntity.ok(connectionService.getConnections());
    }
}
