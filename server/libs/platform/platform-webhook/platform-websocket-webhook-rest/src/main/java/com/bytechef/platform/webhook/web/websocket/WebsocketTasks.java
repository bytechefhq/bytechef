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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@code websocketTasks} realtime pipeline (the embedded voice sub-workflow, as a JSON string) from a
 * workflow trigger.
 *
 * <p>
 * <b>The canonical placement is a trigger extension</b> — a sibling of {@code type}/{@code name}, NOT a member of
 * {@code parameters}:
 *
 * <pre>{@code
 * "triggers": [{
 *   "name": "trigger_1",
 *   "type": "browser/v1/voiceSession/v1",
 *   "websocketTasks": "{\"tasks\":[ ... ]}",
 *   "parameters": { ... }
 * }]
 * }</pre>
 *
 * <p>
 * It belongs in extensions because it is not a component-declared trigger property. Everything in {@code parameters} is
 * treated as one: the property editor round-trips it, and the node description / dynamic property / option / output
 * facades all pass evaluated parameters into component-definition calls. An embedded sub-workflow riding along in that
 * surface is an undeclared passenger in every one of those paths.
 *
 * <p>
 * The {@code parameters} placement is nonetheless accepted, because it is what shipped in
 * {@code docs/examples/voice/deepgram-voiceagent.json} and what the client's voice-capability check reads — rejecting
 * it would break working workflows to make a point. It logs at WARN so the divergence is visible rather than silent.
 *
 * @author Ivica Cardic
 */
final class WebsocketTasks {

    private static final Logger log = LoggerFactory.getLogger(WebsocketTasks.class);

    static final String WEBSOCKET_TASKS = "websocketTasks";

    private WebsocketTasks() {
    }

    /**
     * Returns the trigger's realtime pipeline definition, or {@code null} when the trigger declares none.
     */
    static @Nullable String resolve(WorkflowTrigger workflowTrigger) {
        String definition = workflowTrigger.getExtension(WEBSOCKET_TASKS, String.class, null);

        if (!isBlank(definition)) {
            return definition;
        }

        definition = MapUtils.getString(workflowTrigger.getParameters(), WEBSOCKET_TASKS);

        if (isBlank(definition)) {
            return null;
        }

        if (log.isWarnEnabled()) {
            log.warn(
                "Trigger '{}' declares websocketTasks under parameters; the canonical placement is a trigger " +
                    "extension (a sibling of \"type\"). Support for the parameters placement is retained for " +
                    "compatibility.",
                workflowTrigger.getName());
        }

        return definition;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
