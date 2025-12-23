/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.web.rest.service;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
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
        value = "/get-connection/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Connection> getConnection(@PathVariable long id) {
        return ResponseEntity.ok(connectionService.getConnection(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-connections/{type}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Connection>> getConnections(@PathVariable PlatformType type) {
        return ResponseEntity.ok(connectionService.getConnections(type));
    }
}
