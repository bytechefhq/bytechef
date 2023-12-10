/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.execution.service.CounterService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
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
@RequestMapping("/remote/counter-service")
public class RemoteCounterServiceController {

    private final CounterService counterService;

    @SuppressFBWarnings("EI")
    public RemoteCounterServiceController(CounterService counterService) {
        this.counterService = counterService;
    }

    @RequestMapping(
        method = RequestMethod.DELETE,
        value = "/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        counterService.delete(id);

        return ResponseEntity
            .noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/decrement/{id}")
    public ResponseEntity<Void> decrement(@PathVariable long id) {
        counterService.decrement(id);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/set/{id}/{value}")
    public ResponseEntity<Void> set(@PathVariable long id, @PathVariable long value) {
        counterService.set(id, value);

        return ResponseEntity.noContent()
            .build();
    }
}
