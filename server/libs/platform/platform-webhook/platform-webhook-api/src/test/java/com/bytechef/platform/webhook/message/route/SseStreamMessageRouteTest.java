/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.webhook.message.route;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SseStreamMessageRouteTest {

    @Test
    void sseStreamEventsRouteMustBeOrdered() {
        // In-memory broker dispatches unordered routes on a shared thread pool; SSE tokens would
        // reach the emitter out of order without this flag. Do not relax it without replacing the
        // ordering guarantee elsewhere in the pipeline.
        assertThat(SseStreamMessageRoute.SSE_STREAM_EVENTS.isOrdered())
            .isTrue();
    }
}
