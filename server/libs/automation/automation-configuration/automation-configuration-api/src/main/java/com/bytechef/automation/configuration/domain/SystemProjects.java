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
 * the caller-supplied project name for the embedded automation bridge, an agent id for Agents). Keeping one shape
 * across all four lets list surfaces hide them with a single predicate instead of each learning every feature's private
 * naming scheme.
 *
 * <p>
 * The prefix is matched against an explicit {@link #NAME_PREFIXES} allow-list rather than a bare {@code "__"} test, so
 * a user who legitimately names a project {@code __scratch} still sees it in their lists.
 *
 * <p>
 * {@link #projectNameNotLikePredicates(String)} is the single source of truth for excluding system projects from a
 * hand-built SQL query; repositories should build their {@code project.name} exclusions from it instead of hand-copying
 * the prefix list. {@link #API_COLLECTION_DEPLOYMENT_NAME_PREFIX}, {@link #MCP_SERVER_DEPLOYMENT_NAME_PREFIX} and
 * {@link #A2A_SERVER_DEPLOYMENT_NAME_PREFIX} are a related but distinct namespace — they mark
 * {@code project_deployment.name}, not {@code project.name} — so they are excluded from {@link #NAME_PREFIXES} and must
 * be applied with {@link #notLikePredicate(String, String)} individually.
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

    /**
     * Owns the hidden backing project of one Agent.
     */
    public static final String AI_AGENT_NAME_PREFIX = "__AI_AGENT__";

    /**
     * The deployment-era marker the embedded bridge writes into {@code project.name}. Distinct from
     * {@link #EMBEDDED_AUTOMATION_NAME_PREFIX} and deliberately NOT part of {@link #NAME_PREFIXES} — list surfaces
     * include or exclude it explicitly via the {@code embedded} flag rather than hiding it unconditionally. Mirrors the
     * {@code MARKER} constant in embedded-configuration-service's connected-user facades.
     */
    public static final String EMBEDDED_DEPLOYMENT_NAME_PREFIX = "__EMBEDDED__";

    /**
     * Marks a {@code project_deployment.name}, not a project name — set by the API Platform's ApiCollectionFacadeImpl
     * when it deploys the hidden project backing an API collection.
     */
    public static final String API_COLLECTION_DEPLOYMENT_NAME_PREFIX = "__API_COLLECTION__";

    /**
     * Marks a {@code project_deployment.name}, not a project name — set by the MCP facade when it deploys the hidden
     * project backing an MCP server. Mirrors {@code McpServer.MCP_SERVER_NAME_PREFIX} in platform-mcp-api, duplicated
     * here rather than imported since automation-configuration does not depend on platform-mcp.
     */
    public static final String MCP_SERVER_DEPLOYMENT_NAME_PREFIX = "__MCP_SERVER__";

    /**
     * Marks a {@code project_deployment.name}, not a project name — set by {@code A2aProjectFacadeImpl} when it deploys
     * the hidden project backing an A2A server. Duplicated here rather than imported since automation-configuration
     * does not depend on automation-ai-a2a.
     */
    public static final String A2A_SERVER_DEPLOYMENT_NAME_PREFIX = "__A2A_SERVER__";

    private static final String LIKE_ESCAPE = "\\";

    private static final List<String> NAME_PREFIXES = List.of(
        KNOWLEDGE_BASE_NAME_PREFIX, CONTEXT_STORE_NAME_PREFIX, EMBEDDED_AUTOMATION_NAME_PREFIX,
        AI_AGENT_NAME_PREFIX);

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

    /**
     * One {@code AND columnRef NOT LIKE '...' ESCAPE '\'} SQL fragment per entry in {@link #NAME_PREFIXES}, so a
     * repository can hide every known system project by name without hand-copying the allow-list. The {@code _}
     * characters in each prefix are LIKE wildcards, so they are escaped.
     *
     * @param columnRef the SQL column reference to filter, e.g. {@code "project.name"}
     */
    public static String projectNameNotLikePredicates(String columnRef) {
        StringBuilder predicates = new StringBuilder();

        for (String namePrefix : NAME_PREFIXES) {
            predicates.append(notLikePredicate(columnRef, namePrefix));
        }

        return predicates.toString();
    }

    /**
     * A single {@code AND columnRef NOT LIKE '...' ESCAPE '\'} SQL fragment excluding names starting with
     * {@code prefix}, escaping the {@code _} LIKE wildcard characters the prefix contains.
     */
    public static String notLikePredicate(String columnRef, String prefix) {
        return "AND " + notLikeCondition(columnRef, prefix);
    }

    /**
     * The bare {@code columnRef NOT LIKE '...' ESCAPE '\'} condition, without a leading {@code AND}, for callers
     * splicing it straight after a {@code WHERE}.
     */
    public static String notLikeCondition(String columnRef, String prefix) {
        return "%s NOT LIKE '%s%%' ESCAPE '%s' ".formatted(columnRef, escapeLikeWildcards(prefix), LIKE_ESCAPE);
    }

    /**
     * Positive twin of {@link #notLikeCondition}: {@code columnRef LIKE '...' ESCAPE '\'}, for a caller selecting
     * <em>only</em> the marked rows.
     */
    public static String likeCondition(String columnRef, String prefix) {
        return "%s LIKE '%s%%' ESCAPE '%s' ".formatted(columnRef, escapeLikeWildcards(prefix), LIKE_ESCAPE);
    }

    private static String escapeLikeWildcards(String value) {
        return value.replace("_", LIKE_ESCAPE + "_")
            .replace("%", LIKE_ESCAPE + "%");
    }
}
