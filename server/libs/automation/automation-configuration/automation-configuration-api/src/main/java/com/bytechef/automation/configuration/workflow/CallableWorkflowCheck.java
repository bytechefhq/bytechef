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

package com.bytechef.automation.configuration.workflow;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The answer of {@link CallableWorkflowChecker#check(String)}: what keeps the workflow from being app-callable (empty
 * when it is) and the trigger's input schema when one was found.
 *
 * @author Ivica Cardic
 */
public record CallableWorkflowCheck(List<String> problems, @Nullable String inputSchema) {

    public CallableWorkflowCheck {
        problems = List.copyOf(problems);
    }

    public boolean callable() {
        return problems.isEmpty();
    }
}
