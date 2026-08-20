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

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.polyglot.ContextProxyObject;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotValues;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
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

        try (Context polyglotContext = PolyglotSandbox.newContext(getEngine(), languageId)) {
            polyglotContext.eval(languageId, inputParameters.getString(SCRIPT, switch (languageId) {
                case "java" ->
                    "public static Object perform(Map<String, ?> input, Context context) {\n\treturn null;\n}";
                case "js" -> "function perform(input, context) {\n\treturn null;\n}";
                case "python" -> "def perform(input, context):\n\treturn null";
                case "R" -> "perform <- function(input, context) {\n\treturn null\n}";
                // RUBY-DISABLED: org.graalvm.polyglot:ruby is stuck at 25.0.0 and crashes on the pinned
                // Truffle 25.2.4; re-enable when a ruby jar built on Truffle 25.2+ ships (or GraalVM is
                // downgraded). Grep RUBY-DISABLED.
//              case "ruby" -> "def perform(input, context)\n\treturn null;\nend";
                default -> throw new IllegalArgumentException("languageId: %s does not exist".formatted(languageId));
            }));

            Map<String, Object> inputMap = removeNotEvaluatedEntries(
                inputParameters.getMap(INPUT, Object.class, Map.of()));

            ScriptComponentCatalog componentCatalog = new ScriptComponentCatalog(applicationContext);
            ComponentActionInvoker componentActionInvoker = new ScriptComponentActionInvoker(
                applicationContext, componentConnections, jobContextAware, componentCatalog);

            ContextProxyObject contextProxyObject = new ContextProxyObject(
                languageId, componentActionInvoker, componentCatalog);

            Value value = polyglotContext.getBindings(languageId)
                .getMember("perform")
                .execute(PolyglotValues.copyToGuestValue(inputMap, languageId), contextProxyObject);

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(value));
        }
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

}
