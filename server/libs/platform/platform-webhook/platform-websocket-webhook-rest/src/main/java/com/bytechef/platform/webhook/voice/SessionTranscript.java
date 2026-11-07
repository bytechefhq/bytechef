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

package com.bytechef.platform.webhook.voice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class SessionTranscript {

    static final int MAX_ENTRIES = 2000;

    private final List<Map<String, Object>> entries = new ArrayList<>();
    private final List<Map<String, Object>> toolCalls = new ArrayList<>();

    public synchronized void user(String text) {
        add("user", text);
    }

    public synchronized void assistant(String text) {
        add("assistant", text);
    }

    public synchronized void toolCall(String name, Map<String, ?> arguments, String result) {
        if (toolCalls.size() >= MAX_ENTRIES) {
            return;
        }

        Map<String, Object> toolCall = new LinkedHashMap<>();

        toolCall.put("name", name);
        toolCall.put("arguments", arguments);
        toolCall.put("result", result);
        toolCall.put("at", Instant.now()
            .toString());

        toolCalls.add(toolCall);
    }

    public synchronized List<Map<String, Object>> entries() {
        return List.copyOf(entries);
    }

    public synchronized List<Map<String, Object>> toolCalls() {
        return List.copyOf(toolCalls);
    }

    private void add(String role, String text) {
        if (entries.size() >= MAX_ENTRIES) {
            return;
        }

        Map<String, Object> entry = new LinkedHashMap<>();

        entry.put("role", role);
        entry.put("text", text);
        entry.put("at", Instant.now()
            .toString());

        entries.add(entry);
    }
}
