/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.platform.component.definition.ParameterConnection;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
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
import java.util.Optional;
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
        String languageId, Parameters inputParameters, Map<String, ? extends ParameterConnection> parameterConnections,
        ActionContext actionContext) {

        try (Context polyglotContext = Context
            .newBuilder()
            .engine(engine)
            .build()) {

            polyglotContext
                .getBindings(languageId)
                .putMember(
                    "component",
                    new ComponentProxyObject(actionContext, applicationContext, languageId, parameterConnections));

            polyglotContext.eval(languageId, inputParameters.getRequiredString(SCRIPT));

            Value value = polyglotContext
                .getBindings(languageId)
                .getMember("perform")
                .execute(copyToGuestValue(inputParameters.getMap(INPUT, Object.class), languageId));

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
        if (object instanceof Map<?, ?> map) {
            Map<String, Object> hashMap = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                hashMap.put((String) entry.getKey(), copyFromPolyglotContext(entry.getValue()));
            }

            return hashMap;
        } else if (object instanceof List<?> list) {
            List<Object> arrayList = new ArrayList<>();

            for (Object item : list) {
                arrayList.add(copyFromPolyglotContext(item));
            }

            return arrayList;
        }

        return object;
    }

    private static Object copyToJavaValue(Value value) {
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

    private record ActionProxyObject(
        ActionContext actionContext, ApplicationContext applicationContext, ComponentDefinition componentDefinition,
        String languageId, Map<String, ? extends ParameterConnection> parameterConnections) implements ProxyObject {

        @Override
        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        public ProxyExecutable getMember(String actionName) {
            return arguments -> {
                Map<String, ?> inputParameters = arguments.length == 0
                    ? Map.of()
                    : (Map<String, ?>) copyToJavaValue(arguments[0]);

                Map<String, ComponentConnection> connections;

                if (parameterConnections.isEmpty()) {
                    connections = Map.of();
                } else {
                    if (arguments.length < 2) {
                        connections = getFirstComponentConnectionEntry()
                            .map(Map::ofEntries)
                            .orElse(Map.of());
                    } else {
                        connections = Map.ofEntries(getComponentConnectionEntry(arguments[1].asString()));
                    }
                }

                ActionDefinitionService actionDefinitionService = applicationContext.getBean(
                    ActionDefinitionService.class);

                // TODO latest version should be used

                Object result = actionDefinitionService.executePerform(
                    componentDefinition.getName(), 1, actionName, (Map) copyFromPolyglotContext(inputParameters),
                    connections, actionContext);

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
            return componentDefinition
                .getActions()
                .stream()
                .anyMatch(actionDefinition -> Objects.equals(actionDefinition.getName(), actionName));
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }

        private Map.Entry<String, ComponentConnection> getComponentConnectionEntry(String connectionName) {
            return parameterConnections
                .entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getKey(), connectionName))
                .findFirst()
                .map(entry -> Map.entry(entry.getKey(), toComponentConnection(entry.getValue())))
                .orElseThrow(IllegalArgumentException::new);
        }

        private Optional<Map.Entry<String, ComponentConnection>> getFirstComponentConnectionEntry() {
            return parameterConnections
                .entrySet()
                .stream()
                .filter(entry -> {
                    ParameterConnection parameterConnection = entry.getValue();

                    return Objects.equals(parameterConnection.getComponentName(), componentDefinition.getName());
                })
                .findFirst()
                .map(entry -> Map.entry(entry.getKey(), toComponentConnection(entry.getValue())));
        }

        private ComponentConnection toComponentConnection(ParameterConnection parameterConnection) {
            return new ComponentConnection(
                parameterConnection.getComponentName(), parameterConnection.getVersion(),
                parameterConnection.getParameters(), parameterConnection.getAuthorizationName());
        }
    }

    private static class ComponentProxyObject implements ProxyObject {

        private final ActionContext actionContext;
        private final ApplicationContext applicationContext;
        private final Map<String, ComponentDefinition> componentDefinitionMap = new ConcurrentHashMap<>();
        private final String languageId;
        private final Map<String, ? extends ParameterConnection> parameterConnections;

        private ComponentProxyObject(
            ActionContext actionContext, ApplicationContext applicationContext, String languageId,
            Map<String, ? extends ParameterConnection> parameterConnections) {

            this.actionContext = actionContext;
            this.applicationContext = applicationContext;
            this.languageId = languageId;
            this.parameterConnections = parameterConnections;
        }

        @Override
        public Object getMember(String componentName) {
            return new ActionProxyObject(
                actionContext, applicationContext, componentDefinitionMap.get(componentName), languageId,
                parameterConnections);
        }

        @Override
        public Object getMemberKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMember(String componentName) {
            ComponentDefinitionService componentDefinitionService = applicationContext.getBean(
                ComponentDefinitionService.class);

            // TODO latest version should be used
            componentDefinitionMap.computeIfAbsent(
                componentName, key -> componentDefinitionService.getComponentDefinition(key, 1));

            return componentDefinitionMap.containsKey(componentName);
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException();
        }
    }
}
