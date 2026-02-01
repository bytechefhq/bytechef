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

package com.bytechef.component.script.engine;

import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstant;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyTime;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class PolyglotEngine {

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static Engine engine;

    private final ApplicationContext applicationContext;

    public PolyglotEngine(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object execute(
        String languageId, Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        JobContextAware jobContextAware) {

        try (Context polyglotContext = getContext()) {
            polyglotContext.eval(languageId, inputParameters.getString(SCRIPT, switch (languageId) {
                case "java" ->
                    "public static Object perform(Map<String, ?> input, Context context) {\n\treturn null;\n}";
                case "js" -> "function perform(input, context) {\n\treturn null;\n}";
                case "python" -> "def perform(input, context):\n\treturn null";
                case "R" -> "perform <- function(input, context) {\n\treturn null\n}";
                case "ruby" -> "def perform(input, context)\n\treturn null;\nend";
                default -> throw new IllegalArgumentException("languageId: %s does not exist".formatted(languageId));
            }));

            Map<String, Object> inputMap = removeNotEvaluatedEntries(
                inputParameters.getMap(INPUT, Object.class, Map.of()));
            ContextProxyObject contextProxyObject = new ContextProxyObject(
                languageId, componentConnections, jobContextAware);

            Value value = polyglotContext.getBindings(languageId)
                .getMember("perform")
                .execute(copyToGuestValue(inputMap, languageId), contextProxyObject);

            return copyFromPolyglotContext(copyToJavaValue(value));
        }
    }

    /**
     * Recursively converts data structures originating from a polyglot context into Java-native objects. Handles
     * conversions for common data structures such as maps and lists, while leaving other object types unmodified.
     *
     * @param object the object to be copied and converted from the polyglot context. This may be a map, list, or any
     *               other data type.
     * @return a Java-native representation of the input object. If the input is a map or list, it is recursively copied
     *         and converted. Other objects are returned as is.
     */
    private static Object copyFromPolyglotContext(Object object) {
        switch (object) {
            case null -> {
                return Map.of();
            }
            case Map<?, ?> map -> {
                Map<String, Object> hashMap = new HashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    hashMap.put((String) entry.getKey(), copyFromPolyglotContext(entry.getValue()));
                }

                return hashMap;
            }
            case List<?> list -> {
                List<Object> arrayList = new ArrayList<>();

                for (Object item : list) {
                    arrayList.add(copyFromPolyglotContext(item));
                }

                return arrayList;
            }
            default -> {
            }
        }

        return object;
    }

    private static Object copyToJavaValue(Value value) {
        if (value == null) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isDate()) {
            return value.asDate();
        } else if (value.isHostObject()) {
            return value.asHostObject();
        } else if (value.isInstant()) {
            return value.asInstant();
        } else if (value.isNull()) {
            return null;
        } else if (value.isNumber()) {
            return value.as(Number.class);
        } else if (value.isTime()) {
            return value.asTime();
        } else if (value.isString()) {
            return value.asString();
        } else if (value.hasArrayElements()) {
            return value.as(List.class);
        } else if (value.hasMembers()) {
            return value.as(Map.class);
        } else if (value.isProxyObject()) {
            return value.asProxyObject();
        }

        throw new IllegalArgumentException("Cannot copy value %s to java type.".formatted(value));
    }

    private static Object copyToGuestValue(Object value, String languageId) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            return ProxyArray.fromArray((Object[]) value);
        } else if (value instanceof Boolean bool) {
            return bool;
        } else if (value instanceof Collection<?> collection) {
            List<Object> proxyList = new ArrayList<>();

            for (Object item : collection) {
                proxyList.add(copyToGuestValue(item, languageId));
            }

            return ProxyArray.fromList(proxyList);
        } else if (value instanceof Date date) {
            return ProxyInstant.from(date.toInstant());
        } else if (value instanceof Instant instant) {
            return ProxyInstant.from(instant);
        } else if (value instanceof LocalDate localDate) {
            return ProxyDate.from(localDate);
        } else if (value instanceof LocalTime localTime) {
            return ProxyTime.from(localTime);
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Object> proxyMap = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                proxyMap.put((String) entry.getKey(), copyToGuestValue(entry.getValue(), languageId));
            }

            return ProxyObject.fromMap(proxyMap);
        } else if (value instanceof Number number) {
            return number;
        } else if (value instanceof String string) {
            return string;
        } else if (ConvertUtils.canConvert(value, Map.class)) {
            Map<String, Object> proxyMap = new HashMap<>();

            Map<?, ?> map = ConvertUtils.convertValue(value, Map.class);

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                proxyMap.put((String) entry.getKey(), copyToGuestValue(entry.getValue(), languageId));
            }

            return ProxyObject.fromMap(proxyMap);
        } else {
            throw new IllegalArgumentException("Cannot copy value %s to %s type.".formatted(value, languageId));
        }
    }

    private static Context getContext() {
        return Context.newBuilder()
            .engine(getEngine())
            .build();
    }

    private static Engine getEngine() {
        if (engine == null) {
            LOCK.lock();

            try {
                if (engine == null) {
                    engine = Engine.newBuilder()
                        .build();
                }
            } finally {
                LOCK.unlock();
            }
        }

        return engine;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> removeNotEvaluatedEntries(Map<String, Object> result) {
        Map<String, Object> newMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (entry.getValue() instanceof String string) {
                if (!string.startsWith("${")) {
                    newMap.put(entry.getKey(), entry.getValue());
                }
            } else if (entry.getValue() instanceof Map<?, ?> map) {
                newMap.put(entry.getKey(), removeNotEvaluatedEntries((Map<String, Object>) map));
            } else {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }

        return newMap;
    }

    private final class ActionProxyObject implements ProxyObject {

        private final String languageId;
        private final ComponentDefinition componentDefinition;
        private final Map<String, ComponentConnection> componentConnections;
        private final JobContextAware jobContextAware;

        private ActionProxyObject(
            String languageId, ComponentDefinition componentDefinition,
            Map<String, ComponentConnection> componentConnections, JobContextAware jobContextAware) {

            this.languageId = languageId;
            this.componentDefinition = componentDefinition;
            this.componentConnections = componentConnections;
            this.jobContextAware = jobContextAware;
        }

        @Override
        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        public ProxyExecutable getMember(String actionName) {
            return arguments -> {
                Map<String, ?> inputParameters = Map.of();

                if (arguments.length > 0) {
                    inputParameters = (Map<String, ?>) copyToJavaValue(arguments[0]);
                }

                ComponentConnection componentConnection = null;

                if (!componentConnections.isEmpty()) {
                    Map.Entry<String, ComponentConnection> entry;

                    if (arguments.length < 2) {
                        entry = getFirstComponentConnectionEntry();
                    } else {
                        entry = getComponentConnectionEntry(arguments[1].asString());
                    }

                    if (entry != null) {
                        componentConnection = entry.getValue();
                    }
                }

                ActionDefinitionService actionDefinitionService = applicationContext.getBean(
                    ActionDefinitionService.class);

                ActionContext newActionContext = jobContextAware.toActionContext(
                    componentDefinition.getName(), componentDefinition.getVersion(), actionName,
                    componentConnection);

                Object result = actionDefinitionService.executePerformForPolyglot(
                    componentDefinition.getName(), componentDefinition.getVersion(), actionName,
                    (Map) copyFromPolyglotContext(inputParameters), componentConnection,
                    jobContextAware.getEnvironmentId(), newActionContext);

                if (result == null) {
                    return null;
                }

                return copyToGuestValue(result, languageId);
            };
        }

        @Override
        public Object getMemberKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMember(String actionName) {
            return componentDefinition.getActions()
                .stream()
                .anyMatch(actionDefinition -> Objects.equals(actionDefinition.getName(), actionName));
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }

        private Map.Entry<String, ComponentConnection> getComponentConnectionEntry(String connectionName) {
            return componentConnections
                .entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getKey(), connectionName))
                .findFirst()
                .map(entry -> Map.entry(entry.getKey(), toComponentConnection(entry.getValue())))
                .orElseThrow(() -> new IllegalArgumentException(
                    "Connection with name %s does not exist".formatted(connectionName)));
        }

        private @Nullable Map.Entry<String, ComponentConnection> getFirstComponentConnectionEntry() {
            return componentConnections.entrySet()
                .stream()
                .filter(entry -> {
                    ComponentConnection componentConnection = entry.getValue();

                    return Objects.equals(componentConnection.getComponentName(), componentDefinition.getName());
                })
                .findFirst()
                .map(entry -> Map.entry(entry.getKey(), toComponentConnection(entry.getValue())))
                .orElse(null);
        }

        private ComponentConnection toComponentConnection(ComponentConnection componentConnection) {
            return new ComponentConnection(
                componentConnection.getComponentName(), componentConnection.getVersion(),
                componentConnection.getConnectionId(), componentConnection.getParameters(),
                componentConnection.getAuthorizationType());
        }

        public String languageId() {
            return languageId;
        }

        public ComponentDefinition componentDefinition() {
            return componentDefinition;
        }

        public Map<String, ComponentConnection> componentConnections() {
            return componentConnections;
        }

        public JobContextAware jobContextAware() {
            return jobContextAware;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;
            var that = (ActionProxyObject) obj;
            return Objects.equals(this.languageId, that.languageId) &&
                Objects.equals(this.componentDefinition, that.componentDefinition) &&
                Objects.equals(this.componentConnections, that.componentConnections) &&
                Objects.equals(this.jobContextAware, that.jobContextAware);
        }

        @Override
        public int hashCode() {
            return Objects.hash(languageId, componentDefinition, componentConnections, jobContextAware);
        }

        @Override
        public String toString() {
            return "ActionProxyObject[" +
                "languageId=" + languageId + ", " +
                "componentDefinition=" + componentDefinition + ", " +
                "componentConnections=" + componentConnections + ", " +
                "jobContextAware=" + jobContextAware + ']';
        }

    }

    private class ComponentProxyObject implements ProxyObject {

        private final Map<String, ComponentDefinition> componentDefinitionMap = new ConcurrentHashMap<>();
        private final Map<String, ComponentConnection> componentConnections;
        private final JobContextAware jobContextAware;
        private final String languageId;

        private ComponentProxyObject(
            String languageId, Map<String, ComponentConnection> componentConnections, JobContextAware jobContextAware) {

            this.componentConnections = componentConnections;
            this.jobContextAware = jobContextAware;
            this.languageId = languageId;
        }

        @Override
        public Object getMember(String componentName) {
            return new ActionProxyObject(
                languageId, componentDefinitionMap.get(componentName), componentConnections, jobContextAware);
        }

        @Override
        public Object getMemberKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMember(String componentName) {
            ComponentDefinitionService componentDefinitionService = applicationContext.getBean(
                ComponentDefinitionService.class);

            componentDefinitionMap.computeIfAbsent(
                componentName, key -> componentDefinitionService.getComponentDefinition(key, null));

            return componentDefinitionMap.containsKey(componentName);
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }
    }

    private final class ContextProxyObject implements ProxyObject {

        private final String languageId;
        private final Map<String, ComponentConnection> componentConnections;
        private final JobContextAware jobContextAware;

        private ContextProxyObject(
            String languageId, Map<String, ComponentConnection> componentConnections, JobContextAware jobContextAware) {
            this.languageId = languageId;
            this.componentConnections = componentConnections;
            this.jobContextAware = jobContextAware;
        }

        @Override
        public Object getMember(String name) {
            if (Objects.equals(name, "component")) {
                return new ComponentProxyObject(languageId, componentConnections, jobContextAware);
            }

            return null;
        }

        @Override
        public Object getMemberKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMember(String name) {
            return Objects.equals(name, "component");
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }

        public String languageId() {
            return languageId;
        }

        public Map<String, ComponentConnection> componentConnections() {
            return componentConnections;
        }

        public JobContextAware jobContextAware() {
            return jobContextAware;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;
            var that = (ContextProxyObject) obj;
            return Objects.equals(this.languageId, that.languageId) &&
                Objects.equals(this.componentConnections, that.componentConnections) &&
                Objects.equals(this.jobContextAware, that.jobContextAware);
        }

        @Override
        public int hashCode() {
            return Objects.hash(languageId, componentConnections, jobContextAware);
        }

        @Override
        public String toString() {
            return "ContextProxyObject[" +
                "languageId=" + languageId + ", " +
                "componentConnections=" + componentConnections + ", " +
                "jobContextAware=" + jobContextAware + ']';
        }

    }
}
