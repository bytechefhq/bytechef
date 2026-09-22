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

package com.bytechef.platform.audit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record AuditInvocation(
    String event, Map<String, @Nullable Object> arguments, @Nullable Object result, @Nullable Object captured,
    AuditOutcome outcome, @Nullable Class<? extends Throwable> errorClass) {

    public AuditInvocation {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(outcome, "outcome");

        if ((errorClass != null) != (outcome == AuditOutcome.ERROR)) {
            throw new IllegalArgumentException("errorClass must be set exactly when the outcome is ERROR");
        }

        arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }

    public @Nullable Object argument(String name) {
        return arguments.get(name);
    }

    public Object requireArgument(String name) {
        if (!arguments.containsKey(name)) {
            throw new IllegalArgumentException(
                "Event " + event + " has no argument named " + name + "; available: " + arguments.keySet());
        }

        Object value = arguments.get(name);

        if (value == null) {
            throw new IllegalArgumentException("Event " + event + " argument " + name + " is null");
        }

        return value;
    }
}
