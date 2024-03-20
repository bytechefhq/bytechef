/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
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
@RequestMapping("/remote/context-service")
public class RemoteContextServiceController {

    private final ContextService contextService;

    @SuppressFBWarnings("EI")
    public RemoteContextServiceController(ContextService contextService) {
        this.contextService = contextService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/peek/{stackId}/{classname}",
        produces = {
            "application/json"
        })
    public ResponseEntity<FileEntry> peek(@PathVariable long stackId, @PathVariable Classname classname) {
        return ResponseEntity.ok(contextService.peek(stackId, classname));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/peek/{stackId}/{subStackId}/{classname}",
        produces = {
            "application/json"
        })
    public ResponseEntity<FileEntry> peek(
        @PathVariable long stackId, @PathVariable int subStackId, @PathVariable Classname classname) {

        return ResponseEntity.ok(contextService.peek(stackId, subStackId, classname));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/push/{stackId}/{classname}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> push(
        @PathVariable long stackId, @PathVariable Classname classname, @Valid @RequestBody FileEntry value) {

        contextService.push(stackId, classname, value);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/push/{stackId}/{subStackId}/{classname}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> push(
        @PathVariable long stackId, @PathVariable int subStackId, @PathVariable Classname classname,
        @Valid @RequestBody FileEntry value) {

        contextService.push(stackId, subStackId, classname, value);

        return ResponseEntity.noContent()
            .build();
    }
}
