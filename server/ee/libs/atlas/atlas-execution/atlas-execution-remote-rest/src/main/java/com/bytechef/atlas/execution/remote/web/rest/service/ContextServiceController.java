
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.service.ContextService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class ContextServiceController {

    private final ContextService contextService;

    @SuppressFBWarnings("EI")
    public ContextServiceController(ContextService contextService) {
        this.contextService = contextService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/context-service/peek/{stackId}/{classname}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Map<String, Object>> peek(@PathVariable long stackId, @PathVariable Classname classname) {
        return ResponseEntity.ok(contextService.peek(stackId, classname));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/context-service/peek/{stackId}/{subStackId}/{classname}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Map<String, Object>> peek(
        @PathVariable long stackId, @PathVariable int subStackId, @PathVariable Classname classname) {

        return ResponseEntity.ok(contextService.peek(stackId, subStackId, classname));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/context-service/push/{stackId}/{classname}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> push(
        @PathVariable long stackId, @PathVariable Classname classname,
        @Valid @RequestBody Map<String, Object> context) {

        contextService.push(stackId, classname, context);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/context-service/push/{stackId}/{subStackId}/{classname}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> push(
        @PathVariable long stackId, @PathVariable int subStackId, @PathVariable Classname classname,
        @Valid @RequestBody Map<String, Object> context) {

        contextService.push(stackId, subStackId, classname, context);

        return ResponseEntity.noContent()
            .build();
    }
}
