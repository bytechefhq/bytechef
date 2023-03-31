
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

package com.bytechef.atlas.rsocket.controller;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.service.ContextService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Controller
public class ContextServiceRSocketController {

    private final ContextService contextService;

    @SuppressFBWarnings("EI2")
    public ContextServiceRSocketController(ContextService contextService) {
        this.contextService = contextService;
    }

    @MessageMapping("pushStack")
    public Mono<Void> pushStack(Context context) {
        contextService.push(
            context.getStackId(), Context.Classname.valueOf(context.getClassnameId()), context.getValue());

        return Mono.empty();
    }

    @MessageMapping("pushStackWithSubStackId")
    public Mono<Void> pushStackWithSubStackId(Context context) {
        contextService.push(context.getStackId(), context.getSubStackId(),
            Context.Classname.valueOf(context.getClassnameId()), context.getValue());

        return Mono.empty();
    }

    @MessageMapping("pushStackWithValue")
    public Mono<Void> pushStackWithValue(Context context) {
        contextService.push(context.getStackId(), Context.Classname.valueOf(context.getClassnameId()),
            context.getValue());

        return Mono.empty();
    }

    @MessageMapping("peekStack")
    public Mono<Map<String, Object>> peekStack(Context context) {
        return Mono.create(sink -> sink
            .success(contextService.peek(context.getStackId(), Context.Classname.valueOf(context.getClassnameId()))));
    }

    @MessageMapping("peekStackWithSubStackId")
    public Mono<Map<String, Object>> peekStackWithSubStackId(Context context) {
        return Mono.create(sink -> sink
            .success(contextService.peek(context.getStackId(), context.getSubStackId(),
                Context.Classname.valueOf(context.getClassnameId()))));
    }
}
