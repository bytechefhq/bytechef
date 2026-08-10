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

import java.util.List;

/**
 * Naming convention for <em>system projects</em> — projects a feature auto-provisions to own workflows it generates on
 * the user's behalf, rather than projects a user created.
 *
 * <p>
 * Every system project name is {@code __<DOMAIN>__<discriminator>}: a double-underscore-delimited SCREAMING_SNAKE_CASE
 * domain marker followed by whatever the owning feature scopes by (a workspace id for Knowledge Base and Context Store,
 * the caller-supplied project name for the embedded automation bridge). Keeping one shape across all three lets list
 * surfaces hide them with a single predicate instead of each learning every feature's private naming scheme.
 *
 * <p>
 * The prefix is matched against an explicit {@link #NAME_PREFIXES} allow-list rather than a bare {@code "__"} test, so
 * a user who legitimately names a project {@code __scratch} still sees it in their lists.
 *
 * @author Ivica Cardic
 */
public final class SystemProjects {

    /**
     * Owns the auto-generated Knowledge Base source sync workflows of one workspace.
     */
    public static final String KNOWLEDGE_BASE_NAME_PREFIX = "__KNOWLEDGE_BASE__";

    /**
     * Owns the auto-generated Context Store source sync workflows of one workspace.
     */
    public static final String CONTEXT_STORE_NAME_PREFIX = "__CONTEXT_STORE__";

    /**
     * Marks a catalog project deployed through the embedded automation bridge. Unlike the other two this one is not
     * workspace-scoped — the suffix is the caller-supplied project name.
     */
    public static final String EMBEDDED_AUTOMATION_NAME_PREFIX = "__EMBEDDED_AUTOMATION__";

    private static final List<String> NAME_PREFIXES = List.of(
        KNOWLEDGE_BASE_NAME_PREFIX, CONTEXT_STORE_NAME_PREFIX, EMBEDDED_AUTOMATION_NAME_PREFIX);

    private SystemProjects() {
    }

    /**
     * Whether the given project is auto-provisioned by a feature and should therefore stay out of user-facing project,
     * deployment, and search listings.
     */
    public static boolean isSystemProject(Project project) {
        return project != null && isSystemProjectName(project.getName());
    }

    /**
     * Name-based variant of {@link #isSystemProject(Project)}, for callers holding only the name.
     */
    public static boolean isSystemProjectName(String name) {
        if (name == null) {
            return false;
        }

        for (String namePrefix : NAME_PREFIXES) {
            if (name.startsWith(namePrefix)) {
                return true;
            }
        }

        return false;
    }
}
