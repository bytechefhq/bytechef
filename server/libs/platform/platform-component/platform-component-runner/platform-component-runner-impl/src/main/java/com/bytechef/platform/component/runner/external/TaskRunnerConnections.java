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

package com.bytechef.platform.component.runner.external;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The connections a task hands to an external runner, keyed by connection name, and the masker over their values.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskRunnerConnections(
    boolean enabled, Map<String, Map<String, ?>> connections, SecretMasker secretMasker) {

    private static final TaskRunnerConnections DISABLED = new TaskRunnerConnections(
        false, Map.of(), SecretMasker.NONE);

    private static final Set<String> UNMASKED_PARAMETERS = Set.of(
        "authorizationtype", "region", "baseuri", "baseurl", "url", "uri", "instanceurl", "endpoint", "host",
        "hostname", "port", "domain");

    public static TaskRunnerConnections of(TaskRunnerRequest request, ApplicationProperties applicationProperties) {
        ApplicationProperties.Script script = applicationProperties.getScript();

        if (!script.isInjectConnections()) {
            return DISABLED;
        }

        Map<String, Map<String, ?>> connections = new TreeMap<>();
        List<String> secrets = new ArrayList<>();

        for (Map.Entry<String, ComponentConnection> entry : request.componentConnections()
            .entrySet()) {

            String name = entry.getKey();

            if (TASK_RUNNER.equals(name)) {
                continue;
            }

            ComponentConnection componentConnection = entry.getValue();

            Map<String, Object> parameters = new HashMap<>(componentConnection.getParameters());

            connections.put(name, Collections.unmodifiableMap(parameters));

            secrets.add(JsonUtils.write(parameters));

            for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
                Object value = parameter.getValue();

                if (value != null && !UNMASKED_PARAMETERS.contains(normalize(parameter.getKey()))) {
                    collectStrings(value, secrets);
                }
            }
        }

        if (!connections.isEmpty()) {
            secrets.add(JsonUtils.write(connections));
        }

        return new TaskRunnerConnections(true, Collections.unmodifiableMap(connections), SecretMasker.of(secrets));
    }

    private static void collectStrings(Object value, List<String> secrets) {
        if (value instanceof String string) {
            secrets.add(string);
        } else if (value instanceof Map<?, ?> map) {
            for (Object nestedValue : map.values()) {
                if (nestedValue != null) {
                    collectStrings(nestedValue, secrets);
                }
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object nestedValue : iterable) {
                if (nestedValue != null) {
                    collectStrings(nestedValue, secrets);
                }
            }
        }
    }

    private static String normalize(String name) {
        return name.replaceAll("[_.\\-]", "")
            .toLowerCase(Locale.ROOT);
    }
}
