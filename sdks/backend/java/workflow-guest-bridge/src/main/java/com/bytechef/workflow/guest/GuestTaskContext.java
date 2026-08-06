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

package com.bytechef.workflow.guest;

import com.bytechef.workflow.definition.TaskContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link TaskContext} implementation used inside the GraalVM Espresso guest JVM. Every call crosses the sandbox
 * boundary as JSON through the {@link HostBridge} the loader exported into the polyglot bindings; the host resolves
 * connections and dispatches the component action there.
 *
 * @author Ivica Cardic
 */
public final class GuestTaskContext implements TaskContext {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    private final HostBridge hostBridge;

    public GuestTaskContext() {
        this(resolveHostBridge());
    }

    GuestTaskContext(HostBridge hostBridge) {
        this.hostBridge = hostBridge;
    }

    @Override
    public Object component(
        String componentName, String actionName, Map<String, ?> input, String connectionName,
        Map<String, ?> clusterElements) throws Exception {

        Map<String, Object> request = new LinkedHashMap<>();

        request.put("componentName", componentName);
        request.put("actionName", actionName);
        request.put("input", input == null ? Map.of() : input);

        if (connectionName != null) {
            request.put("connectionName", connectionName);
        }

        if (clusterElements != null && !clusterElements.isEmpty()) {
            request.put("clusterElements", clusterElements);
        }

        String resultJson = hostBridge.componentExecute(OBJECT_MAPPER.writeValueAsString(request));

        if (resultJson == null) {
            return null;
        }

        return OBJECT_MAPPER.readValue(resultJson, Object.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> input() {
        String inputJson = hostBridge.input();

        if (inputJson == null) {
            return Map.of();
        }

        try {
            return OBJECT_MAPPER.readValue(inputJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object input(String name) {
        // Resolved on the host rather than against the snapshot read above, so a missing name gets the host's
        // explanation — including that the task runs concurrently with this one.
        String valueJson = hostBridge.input(name);

        if (valueJson == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(valueJson, Object.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> parameters() {
        String parametersJson = hostBridge.parameters();

        if (parametersJson == null) {
            return Map.of();
        }

        try {
            return OBJECT_MAPPER.readValue(parametersJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> connection(String connectionName) throws Exception {
        String parametersJson = hostBridge.connection(connectionName);

        if (parametersJson == null) {
            return Map.of();
        }

        return OBJECT_MAPPER.readValue(parametersJson, Map.class);
    }

    @Override
    public void log(LogLevel level, String message) {
        hostBridge.log(level.name(), message);
    }

    private static HostBridge resolveHostBridge() {
        Object imported = com.oracle.truffle.espresso.polyglot.Polyglot.importObject("byteChefCodeWorkflowHostBridge");

        return new ForeignHostBridge(imported);
    }

    /**
     * {@link HostBridge} view over the foreign (host) callback object; every call goes through the Espresso guest
     * interop API since foreign objects cannot be cast to guest interfaces directly.
     */
    private record ForeignHostBridge(Object foreignObject) implements HostBridge {

        @Override
        public String componentExecute(String requestJson) {
            try {
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(
                    foreignObject, "componentExecute", requestJson);

                return com.oracle.truffle.espresso.polyglot.Interop.asString(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String input() {
            try {
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(foreignObject, "input");

                return com.oracle.truffle.espresso.polyglot.Interop.asString(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String input(String name) {
            try {
                // The host exports input() and input(String); interop selects between them by arity.
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(
                    foreignObject, "input", name);

                return com.oracle.truffle.espresso.polyglot.Interop.asString(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String parameters() {
            try {
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(foreignObject, "parameters");

                return com.oracle.truffle.espresso.polyglot.Interop.asString(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String connection(String connectionName) {
            try {
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(
                    foreignObject, "connection", connectionName);

                return com.oracle.truffle.espresso.polyglot.Interop.asString(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void log(String level, String message) {
            try {
                com.oracle.truffle.espresso.polyglot.Interop.invokeMember(foreignObject, "log", level, message);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
