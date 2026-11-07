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

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
final class VoiceControlFrame {

    private VoiceControlFrame() {
    }

    static boolean isKeepalive(String payload) {
        if (payload == null || !payload.contains("keepalive")) {
            return false;
        }

        try {
            Map<?, ?> frame = JsonUtils.read(payload, Map.class);

            return "control".equals(frame.get("type")) && "keepalive".equals(frame.get("action"));
        } catch (RuntimeException runtimeException) {
            return false;
        }
    }
}
