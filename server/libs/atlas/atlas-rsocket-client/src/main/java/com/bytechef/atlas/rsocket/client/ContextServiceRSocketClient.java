
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

package com.bytechef.atlas.rsocket.client;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.service.ContextService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextServiceRSocketClient implements ContextService {

    private final RSocketRequester rSocketRequester;

    public ContextServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public void push(long stackId, Context.Classname classname, Map<String, Object> value) {
        Context context = new Context(stackId, classname, value);

        rSocketRequester.route("pushStackWithValue")
            .data(context)
            .send()
            .block();
    }

    @Override
    public void push(long stackId, int subStackId, Context.Classname classname, Map<String, Object> value) {
        Context context = new Context(stackId, subStackId, classname, value);

        rSocketRequester.route("pushStackWithSubStackId")
            .data(context)
            .send()
            .block();
    }

    @Override
    public Map<String, Object> peek(long stackId, Context.Classname classname) {
        return rSocketRequester
            .route("peekStack")
            .data(new Context(stackId, classname))
            .retrieveMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }

    @Override
    public Map<String, Object> peek(long stackId, int subStackId, Context.Classname classname) {
        return rSocketRequester
            .route("peekStackWithSubStack")
            .data(new Context(stackId, subStackId, classname))
            .retrieveMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }
}
