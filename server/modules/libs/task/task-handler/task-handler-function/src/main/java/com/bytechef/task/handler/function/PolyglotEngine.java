/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.handler.function;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class PolyglotEngine {

    public Object execute(String languageId, TaskExecution taskExecution) {
        try (Context polyglotContext = Context.newBuilder().allowAllAccess(true).build()) {

            injectValues(polyglotContext.getBindings(languageId), taskExecution.get("context", Map.class));

            Value value = polyglotContext.eval(languageId, taskExecution.get("source", String.class));

            return value.as(Object.class);
        }
    }

    private void injectValues(Value guestLanguageBingingValue, Map<String, String> taskExecutionContext) {

        for (Map.Entry<String, String> entry : taskExecutionContext.entrySet()) {

            guestLanguageBingingValue.putMember(entry.getKey(), entry.getValue());
        }
    }
}
