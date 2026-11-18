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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The environment variables and the masker that hand a task's declared connections to an external runner.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskRunnerConnectionEnvironment(Map<String, String> variables, SecretMasker secretMasker) {

    static final String PREFIX = "BYTECHEF_CONNECTION_";

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerConnectionEnvironment.class);
    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Set<String> UNMASKED_PARAMETERS = Set.of(
        "authorizationtype", "region", "baseuri", "baseurl", "url", "uri", "instanceurl", "endpoint", "host",
        "hostname", "port", "domain");
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^A-Z0-9_]");

    public static TaskRunnerConnectionEnvironment of(
        TaskRunnerRequest request, ApplicationProperties applicationProperties) {

        ApplicationProperties.Script script = applicationProperties.getScript();

        if (!script.isInjectConnections()) {
            return new TaskRunnerConnectionEnvironment(Map.of(), SecretMasker.NONE);
        }

        Map<String, String> variables = new LinkedHashMap<>();
        Map<String, String> origins = new HashMap<>();
        List<String> secrets = new ArrayList<>();

        Map<String, ComponentConnection> connections = new TreeMap<>(request.componentConnections());

        for (Map.Entry<String, ComponentConnection> entry : connections.entrySet()) {
            String key = entry.getKey();

            if (TASK_RUNNER.equals(key)) {
                continue;
            }

            ComponentConnection componentConnection = entry.getValue();

            Map<String, ?> parameters = componentConnection.getConnectionParameters();

            String connectionVariable = PREFIX + sanitize(key);
            String json = JsonUtils.write(parameters);

            put(variables, origins, connectionVariable, json, "connection '%s'".formatted(key));

            secrets.add(json);

            for (Map.Entry<String, ?> parameter : new TreeMap<>(parameters).entrySet()) {
                String name = parameter.getKey();
                Object value = parameter.getValue();

                if (value == null) {
                    continue;
                }

                if (!UNMASKED_PARAMETERS.contains(normalize(name))) {
                    collectStrings(value, secrets);
                }

                String flatValue = flatValue(value);

                if (flatValue.indexOf('\0') >= 0) {
                    log.warn(
                        "Parameter '{}' of connection '{}' contains a NUL character, so only {} carries it", name, key,
                        connectionVariable);

                    continue;
                }

                put(
                    variables, origins, connectionVariable + "_" + sanitize(name), flatValue,
                    "parameter '%s' of connection '%s'".formatted(name, key));
            }
        }

        return new TaskRunnerConnectionEnvironment(
            Collections.unmodifiableMap(variables), SecretMasker.of(secrets));
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

    private static String flatValue(Object value) {
        if (value instanceof String string) {
            return string;
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return JsonUtils.write(value);
    }

    private static void put(
        Map<String, String> variables, Map<String, String> origins, String variable, String value, String origin) {

        String existingOrigin = origins.putIfAbsent(variable, origin);

        if (existingOrigin != null) {
            throw new IllegalArgumentException(
                "The %s and the %s both map to %s; rename one".formatted(existingOrigin, origin, variable));
        }

        variables.put(variable, value);
    }

    private static String normalize(String name) {
        return name.replaceAll("[_.\\-]", "")
            .toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String name) {
        String wordsSeparated = CAMEL_CASE_BOUNDARY.matcher(name)
            .replaceAll("$1_$2");

        return NOT_ALLOWED.matcher(wordsSeparated.toUpperCase(Locale.ROOT))
            .replaceAll("_");
    }
}
