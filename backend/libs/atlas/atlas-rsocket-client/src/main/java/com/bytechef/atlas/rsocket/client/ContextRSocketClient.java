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
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextRSocketClient implements ContextService {

    private final RSocketRequester rSocketRequester;

    public ContextRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public void push(String stackId, Context context) {
        context.setStackId(stackId);

        rSocketRequester.route("pushStack").data(context).send().block();
    }

    @Override
    public Context peek(String stackId) {
        return rSocketRequester
                .route("peekStack")
                .data(stackId)
                .retrieveMono(Context.class)
                .block();
    }
}
