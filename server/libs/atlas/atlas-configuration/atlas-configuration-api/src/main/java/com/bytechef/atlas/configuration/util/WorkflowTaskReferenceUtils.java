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

package com.bytechef.atlas.configuration.util;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting output property path references from workflow task parameters.
 *
 * <p>
 * Given a target task name, this utility scans all downstream tasks' parameters for {@code ${taskName.property.path}}
 * expressions and collects the referenced property paths.
 *
 * @author Ivica Cardic
 */
public class WorkflowTaskReferenceUtils {

    /**
     * Extracts all output property paths referenced by downstream tasks for the given task name.
     *
     * <p>
     * For example, given task name "accelo_1" and a downstream task with parameter
     * {@code "${accelo_1.response.lastname}"}, this method returns a set containing {@code "response.lastname"}.
     *
     * @param allTasks the complete list of workflow tasks
     * @param taskName the name of the task whose output references to extract
     * @return a set of dot-separated property paths referenced by downstream tasks
     */
    public static Set<String> extractReferencedOutputPaths(List<WorkflowTask> allTasks, String taskName) {
        Set<String> referencedPaths = new LinkedHashSet<>();
        Pattern referencePattern = Pattern.compile("\\$\\{" + Pattern.quote(taskName) + "\\.([^}]+)}");
        boolean foundTask = false;

        for (WorkflowTask workflowTask : allTasks) {
            if (!foundTask) {
                if (taskName.equals(workflowTask.getName())) {
                    foundTask = true;
                }

                continue;
            }

            collectReferencesFromParameters(workflowTask.getParameters(), referencePattern, referencedPaths);
        }

        return referencedPaths;
    }

    private static void collectReferencesFromParameters(
        Map<String, ?> parameters, Pattern referencePattern, Set<String> referencedPaths) {

        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            collectReferencesFromValue(entry.getValue(), referencePattern, referencedPaths);
        }
    }

    private static void collectReferencesFromValue(
        Object value, Pattern referencePattern, Set<String> referencedPaths) {

        if (value instanceof String stringValue) {
            Matcher matcher = referencePattern.matcher(stringValue);

            while (matcher.find()) {
                referencedPaths.add(matcher.group(1));
            }
        } else if (value instanceof Map<?, ?> mapValue) {
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                collectReferencesFromValue(entry.getValue(), referencePattern, referencedPaths);
            }
        } else if (value instanceof Collection<?> collectionValue) {
            for (Object item : collectionValue) {
                collectReferencesFromValue(item, referencePattern, referencedPaths);
            }
        }
    }
}
