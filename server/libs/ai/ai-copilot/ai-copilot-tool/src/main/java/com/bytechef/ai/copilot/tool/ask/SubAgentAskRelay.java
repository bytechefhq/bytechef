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

package com.bytechef.ai.copilot.tool.ask;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * The whole "a specialist may ask the user a question" capability, as one optional seam.
 *
 * @author Ivica Cardic
 */
public interface SubAgentAskRelay {

    /**
     * The specialist-facing {@code askUserQuestion} tool to attach to a delegate's own {@code ChatClient}.
     */
    ToolCallback askUserQuestionToolCallback();

    <T> AskOutcome<T> runWithChannel(Supplier<T> supplier);

    record AskOutcome<T>(@Nullable T result, @Nullable String pendingQuestion) {
    }
}
