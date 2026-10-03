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

package com.bytechef.platform.ai.a2a;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public sealed interface A2AAgentResult {

    @Nullable
    A2ATaskReference taskReference();

    static A2AAgentResult ofCompleted(String text, A2ATaskReference taskReference) {
        return new Completed(text, taskReference);
    }

    static A2AAgentResult ofFailed(String errorMessage) {
        return new Failed(errorMessage, null);
    }

    static A2AAgentResult ofFailed(String errorMessage, A2ATaskReference taskReference) {
        return new Failed(errorMessage, taskReference);
    }

    static A2AAgentResult ofInputRequired(String text, A2ATaskReference taskReference) {
        return new InputRequired(text, taskReference);
    }

    static A2AAgentResult ofWorking(String text, A2ATaskReference taskReference) {
        return new Working(text, taskReference);
    }

    record Completed(String text, @Nullable A2ATaskReference taskReference) implements A2AAgentResult {

        public Completed {
            text = text == null ? "" : text;
        }
    }

    record Failed(String errorMessage, @Nullable A2ATaskReference taskReference) implements A2AAgentResult {

        public Failed {
            errorMessage = errorMessage == null ? "The agent failed without an error message" : errorMessage;
        }
    }

    record InputRequired(String text, A2ATaskReference taskReference) implements A2AAgentResult {

        public InputRequired {
            text = text == null ? "" : text;

            Objects.requireNonNull(taskReference, "taskReference");
        }
    }

    record Working(String text, A2ATaskReference taskReference) implements A2AAgentResult {

        public Working {
            text = text == null ? "" : text;

            Objects.requireNonNull(taskReference, "taskReference");
        }
    }
}
