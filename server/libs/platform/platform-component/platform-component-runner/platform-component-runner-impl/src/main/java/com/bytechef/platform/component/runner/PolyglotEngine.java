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

package com.bytechef.platform.component.runner;

import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.polyglot.ContextProxyObject;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotValues;
import com.bytechef.platform.component.polyglot.ScriptSandboxMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class PolyglotEngine {

    private static final String INPUT = "input";

    private final ApplicationContext applicationContext;

    public PolyglotEngine(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object execute(
        ScriptSandboxMode mode, @Nullable Duration timeout, String languageId, Parameters inputParameters,
        @Nullable Map<String, ComponentConnection> componentConnections, JobContextAware jobContextAware) {

        return execute(mode, timeout, languageId, inputParameters, componentConnections, true, jobContextAware);
    }

    public Object execute(
        ScriptSandboxMode mode, @Nullable Duration timeout, String languageId, Parameters inputParameters,
        @Nullable Map<String, ComponentConnection> componentConnections, boolean exposeConnectionParameters,
        JobContextAware jobContextAware) {

        return PolyglotSandbox.call(mode, languageId, timeout, polyglotContext -> {
            polyglotContext.eval(languageId, inputParameters.getString(SCRIPT, switch (languageId) {
                case "java" ->
                    "public static Object perform(Map<String, ?> input, Context context) {\n\treturn null;\n}";
                case "js" -> "function perform(input, context) {\n\treturn null;\n}";
                case "python" -> "def perform(input, context):\n\treturn null";
                case "R" -> "perform <- function(input, context) {\n\treturn null\n}";
                default -> throw new IllegalArgumentException("languageId: %s does not exist".formatted(languageId));
            }));

            Map<String, ComponentConnection> scriptComponentConnections =
                componentConnections == null ? Map.of() : componentConnections;
            Map<String, Object> inputMap = removeNotEvaluatedEntries(
                inputParameters.getMap(INPUT, Object.class, Map.of()));

            PolyglotComponentCatalog componentCatalog = new PolyglotComponentCatalog(applicationContext);
            ComponentActionInvoker componentActionInvoker = new PolyglotComponentActionInvoker(
                applicationContext, scriptComponentConnections, jobContextAware, componentCatalog);

            ContextProxyObject contextProxyObject = new ContextProxyObject(
                languageId, componentActionInvoker, componentCatalog);

            Value performFunction = polyglotContext.getBindings(languageId)
                .getMember("perform");

            if (performFunction == null || !performFunction.canExecute()) {
                throw new IllegalArgumentException("The script must define a function named 'perform'");
            }

            Value value;

            if (getRequiredPerformParameterCount(polyglotContext, languageId) >= 3) {
                Map<String, ComponentConnection> exposedComponentConnections =
                    exposeConnectionParameters ? scriptComponentConnections : Map.of();

                value = performFunction.execute(
                    PolyglotValues.copyToGuestValue(inputMap, languageId),
                    PolyglotValues.copyToGuestMemberAndHashMap(
                        toConnectionMap(exposedComponentConnections), languageId),
                    contextProxyObject);
            } else {
                value = performFunction.execute(
                    PolyglotValues.copyToGuestValue(inputMap, languageId), contextProxyObject);
            }

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(value));
        });
    }

    private static int getRequiredPerformParameterCount(Context polyglotContext, String languageId) {
        String parameterCountExpression = switch (languageId) {
            case "js" -> "perform.length";
            case "python" -> "perform.__code__.co_argcount - len(perform.__defaults__ or ())";
            default -> null;
        };

        if (parameterCountExpression == null) {
            return 2;
        }

        Value parameterCount = polyglotContext.eval(languageId, parameterCountExpression);

        return parameterCount.fitsInInt() ? parameterCount.asInt() : 2;
    }

    private static Map<String, Object> toConnectionMap(Map<String, ComponentConnection> componentConnections) {
        Map<String, Object> connectionMap = new HashMap<>();

        for (Map.Entry<String, ComponentConnection> entry : componentConnections.entrySet()) {
            ComponentConnection componentConnection = entry.getValue();

            connectionMap.put(entry.getKey(), componentConnection.getParameters());
        }

        return connectionMap;
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

}
