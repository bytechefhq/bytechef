
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

package com.bytechef.hermes.worker.remote.web.rest.trigger.handler;

import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.excepton.TriggerExecutionException;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class TriggerHandlerController {

    private final TriggerHandlerRegistry triggerHandlerRegistry;

    public TriggerHandlerController(TriggerHandlerRegistry triggerHandlerRegistry) {
        this.triggerHandlerRegistry = triggerHandlerRegistry;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-handler/handle",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Object> handle(@Valid @RequestBody TriggerHandlerHandleRequest triggerHandlerHandleRequest) {
        TriggerHandler<?> triggerHandler = triggerHandlerRegistry.getTriggerHandler(triggerHandlerHandleRequest.type());

        try {
            Object output = triggerHandler.handle(triggerHandlerHandleRequest.triggerExecution());

            if (output == null) {
                return ResponseEntity.noContent()
                    .build();
            } else {
                return ResponseEntity.ok(output);
            }
        } catch (TriggerExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    record TriggerHandlerHandleRequest(@NotNull String type, TriggerExecution triggerExecution) {
    }
}
