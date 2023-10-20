
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

package com.bytechef.atlas.rsocket.service;

import com.bytechef.atlas.service.CounterService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class CounterServiceRSocketController {

    private final CounterService counterService;

    @SuppressFBWarnings("EI2")
    public CounterServiceRSocketController(CounterService counterService) {
        this.counterService = counterService;
    }

    @MessageMapping("deleteCounter")
    public Mono<Void> deleteCounter(Long id) {
        counterService.delete(id);

        return Mono.empty();
    }

    @MessageMapping("decrementCounter")
    public Mono<Long> decrementCounter(Long id) {
        return Mono.create(sink -> sink.success(counterService.decrement(id)));
    }

    @MessageMapping("setCounter")
    public Mono<Void> setCounter(Map<String, Object> map) {
        counterService.set((Long) map.get("id"), (Long) map.get("value"));

        return Mono.empty();
    }
}
