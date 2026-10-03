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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record A2AAgentRun(@Nullable A2ATaskReference taskReference, CompletableFuture<A2AAgentResult> result) {

    public A2AAgentRun {
        Objects.requireNonNull(result, "result");
    }

    public static A2AAgentRun of(A2AAgentResult agentResult) {
        return new A2AAgentRun(agentResult.taskReference(), CompletableFuture.completedFuture(agentResult));
    }

    public static A2AAgentRun started(
        A2ATaskReference taskReference, CompletableFuture<A2AAgentResult> result) {

        return new A2AAgentRun(Objects.requireNonNull(taskReference, "taskReference"), result);
    }
}
