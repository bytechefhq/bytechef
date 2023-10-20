
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

package com.bytechef.hermes.worker.rsocket.controller.task.handler;

import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.excepton.TriggerExecutionException;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerAccessor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class TriggerHandlerRSocketController {

    private final TriggerHandlerAccessor triggerHandlerAccessor;

    public TriggerHandlerRSocketController(TriggerHandlerAccessor triggerHandlerAccessor) {
        this.triggerHandlerAccessor = triggerHandlerAccessor;
    }

    @MessageMapping("TriggerHandler.handle")
    public Mono<Object> handle(TriggerHandlerHandleRequest triggerHandlerHandleRequest) {
        TriggerHandler<?> triggerHandler = triggerHandlerAccessor.getTriggerHandler(triggerHandlerHandleRequest.type());

        try {
            Object output = triggerHandler.handle(triggerHandlerHandleRequest.triggerExecution());

            if (output == null) {
                return Mono.empty();
            } else {
                return Mono.just(output);
            }
        } catch (TriggerExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    record TriggerHandlerHandleRequest(String type, TriggerExecution triggerExecution) {
    }
}
