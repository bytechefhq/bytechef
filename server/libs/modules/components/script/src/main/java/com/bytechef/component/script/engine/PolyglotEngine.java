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
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
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

    private final ApplicationContext applicationContext;

    private static final Engine engine = Engine
        .newBuilder()
        .build();

    public PolyglotEngine(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object execute(
        String languageId, Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        ActionContext actionContext) {

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
                actionContext, applicationContext, languageId, componentConnections);

            Value value = polyglotContext.getBindings(languageId)
                .getMember("perform")
                .execute(copyToGuestValue(inputMap, languageId), contextProxyObject);

            return copyFromPolyglotContext(copyToJavaValue(value));
        }
    }

    /**
     * Copy from PolyglotMap to Map and PolyglotList to List
     *
     * @param object
     * @return
     */
    private static Object copyFromPolyglotContext(Object object) {
        switch (object) {
            case null -> {
                return null;
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
            .engine(engine)
            .build();
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

    private record ActionProxyObject(
        ActionContext actionContext, ApplicationContext applicationContext, ComponentDefinition componentDefinition,
        String languageId, Map<String, ComponentConnection> componentConnections) implements ProxyObject {

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

                ActionDefinitionFacade actionDefinitionFacade = applicationContext.getBean(
                    ActionDefinitionFacade.class);

                Object result = actionDefinitionFacade.executePerformForPolyglot(
                    componentDefinition.getName(), componentDefinition.getVersion(), actionName,
                    (Map) copyFromPolyglotContext(inputParameters), componentConnection, actionContext);

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

        private Map.Entry<String, ComponentConnection> getFirstComponentConnectionEntry() {
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
    }

    private static class ComponentProxyObject implements ProxyObject {

        private ActionContext actionContext;
        private final ApplicationContext applicationContext;
        private final Map<String, ComponentDefinition> componentDefinitionMap = new ConcurrentHashMap<>();
        private final String languageId;
        private final Map<String, ComponentConnection> componentConnections;

        private ComponentProxyObject(
            ActionContext actionContext, ApplicationContext applicationContext, String languageId,
            Map<String, ComponentConnection> componentConnections) {

            this.actionContext = actionContext;
            this.applicationContext = applicationContext;
            this.languageId = languageId;
            this.componentConnections = componentConnections;
        }

        @Override
        public Object getMember(String componentName) {
            ContextFactory contextFactory = applicationContext.getBean(ContextFactory.class);

            ActionContextAware actionContextAware = (ActionContextAware) actionContext;

            ComponentDefinition componentDefinition = componentDefinitionMap.get(componentName);

            actionContext = contextFactory.createActionContext(
                componentName,
                componentDefinition.getVersion(),
                "",
                actionContextAware.getJobPrincipalId(),
                actionContextAware.getJobPrincipalWorkflowId(),
                actionContextAware.getJobId(),
                actionContextAware.getWorkflowId(),
                componentConnections.get(componentName),
                actionContextAware.getModeType(),
                true);

            return new ActionProxyObject(
                actionContext, applicationContext, componentDefinitionMap.get(componentName), languageId,
                componentConnections);
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

    private record ContextProxyObject(
        ActionContext actionContext, ApplicationContext applicationContext, String languageId,
        Map<String, ComponentConnection> componentConnections) implements ProxyObject {

        @Override
        public Object getMember(String name) {
            if (Objects.equals(name, "component")) {
                return new ComponentProxyObject(actionContext, applicationContext, languageId, componentConnections);
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
    }
}
