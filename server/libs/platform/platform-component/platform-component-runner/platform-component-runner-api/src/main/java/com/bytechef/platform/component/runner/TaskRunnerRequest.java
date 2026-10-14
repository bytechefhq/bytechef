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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskRunnerRequest(
    String languageId, @Nullable String script, List<String> commands, Map<String, ?> input, Map<String, String> env,
    Map<String, ?> inputFiles, List<String> outputFilePatterns, Parameters inputParameters,
    Parameters runnerParameters, @Nullable Duration timeout,
    Map<String, ComponentConnection> componentConnections,
    ActionContext actionContext) {

    public TaskRunnerRequest {
        if (script == null && commands.isEmpty()) {
            throw new IllegalArgumentException("either script or commands must be given");
        }

        if (script != null && !commands.isEmpty()) {
            throw new IllegalArgumentException("script and commands are mutually exclusive");
        }
    }
}
