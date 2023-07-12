
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

package com.bytechef.hermes.execution.remote.web.rest.service;

import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class TriggerExecutionServiceController {

    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public TriggerExecutionServiceController(TriggerExecutionService triggerExecutionService) {
        this.triggerExecutionService = triggerExecutionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-execution-service/create",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerExecution> create(TriggerExecution triggerExecution) {
        return ResponseEntity.ok(triggerExecutionService.create(triggerExecution));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/trigger-execution-service/get-trigger-execution/{id}",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerExecution> getTriggerExecution(@PathVariable long id) {
        return ResponseEntity.ok(triggerExecutionService.getTriggerExecution(id));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-execution-service/update",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerExecution> update(TriggerExecution triggerExecution) {
        return ResponseEntity.ok(triggerExecutionService.update(triggerExecution));
    }
}
