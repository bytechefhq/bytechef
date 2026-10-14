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

import com.bytechef.component.definition.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskRunnerResult(
    @Nullable Object output, @Nullable Integer exitCode, String stdout, String stderr,
    Map<String, FileEntry> outputFiles) {

    public static TaskRunnerResult ofOutput(@Nullable Object output) {
        return new TaskRunnerResult(output, null, "", "", Map.of());
    }

    public Map<String, Object> toExternalMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(TaskRunnerConstants.RESULT_EXIT_CODE, exitCode);
        map.put(TaskRunnerConstants.RESULT_STDOUT, stdout);
        map.put(TaskRunnerConstants.RESULT_STDERR, stderr);
        map.put(TaskRunnerConstants.RESULT_VARS, output);
        map.put(TaskRunnerConstants.OUTPUT_FILES, outputFiles);

        return map;
    }
}
