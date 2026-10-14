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

package com.bytechef.platform.component.runner.remote;

import java.util.Locale;

public enum RemotePullPolicy {

    ALWAYS, IF_NOT_PRESENT, NEVER;

    public static RemotePullPolicy of(String value) {
        for (RemotePullPolicy candidate : values()) {
            if (candidate.name()
                .equalsIgnoreCase(value.trim())) {

                return candidate;
            }
        }

        throw new IllegalArgumentException("Unknown pull policy '%s'".formatted(value));
    }

    public String getLabel() {
        String lower = name().toLowerCase(Locale.ROOT)
            .replace('_', ' ');

        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
