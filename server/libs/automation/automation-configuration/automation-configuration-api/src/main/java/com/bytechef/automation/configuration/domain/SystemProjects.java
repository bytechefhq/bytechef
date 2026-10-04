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

package com.bytechef.automation.configuration.domain;

/**
 * @author Ivica Cardic
 */
public final class SystemProjects {

    public static final String EMBEDDED_AUTOMATION_NAME_PREFIX = "__EMBEDDED_AUTOMATION__";

    public static final String A2A_SERVER_DEPLOYMENT_NAME_PREFIX = "__A2A_SERVER__";

    private SystemProjects() {
    }

    public static boolean isSystemProject(Project project) {
        return project != null && isSystemProjectName(project.getName());
    }

    public static boolean isSystemProjectName(String name) {
        if (name == null) {
            return false;
        }

        return name.startsWith(EMBEDDED_AUTOMATION_NAME_PREFIX);
    }
}
