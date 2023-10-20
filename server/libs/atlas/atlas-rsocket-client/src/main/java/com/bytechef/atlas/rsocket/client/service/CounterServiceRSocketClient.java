
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

package com.bytechef.atlas.rsocket.client.service;

import com.bytechef.atlas.service.CounterService;
import java.util.Map;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class CounterServiceRSocketClient implements CounterService {

    private final RSocketRequester rSocketRequester;

    public CounterServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public void delete(long id) {
        rSocketRequester.route("deleteCounter")
            .data(id)
            .send()
            .block();
    }

    @Override
    public long decrement(long id) {
        Long counter = rSocketRequester
            .route("decrementCounter")
            .data(id)
            .retrieveMono(Long.class)
            .block();

        return counter == null ? 0 : counter;
    }

    @Override
    public void set(long id, long value) {
        rSocketRequester
            .route("setCounter")
            .data(Map.of("id", id, "value", value))
            .send()
            .block();
    }
}
