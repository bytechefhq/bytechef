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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SystemProjectsTest {

    @Test
    void testEveryPrefixFollowsTheSharedNameShape() {
        for (String namePrefix : new String[] {
            SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX, SystemProjects.CONTEXT_STORE_NAME_PREFIX,
            SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX, SystemProjects.AI_AGENT_NAME_PREFIX
        }) {
            assertTrue(
                namePrefix.matches("__[A-Z][A-Z_]*[A-Z]__"),
                namePrefix + " must be __SCREAMING_SNAKE_CASE__");
        }
    }

    @Test
    void testWorkspaceScopedNamesAreRecognized() {
        assertTrue(SystemProjects.isSystemProjectName(SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX + 1));
        assertTrue(SystemProjects.isSystemProjectName(SystemProjects.CONTEXT_STORE_NAME_PREFIX + 42));
        assertTrue(SystemProjects.isSystemProjectName(SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX + "catalog"));
        assertTrue(SystemProjects.isSystemProjectName(SystemProjects.AI_AGENT_NAME_PREFIX + "x"));
    }

    @Test
    void testUserProjectsAreNotSystemProjects() {
        assertFalse(SystemProjects.isSystemProjectName("My Project"));
        assertFalse(SystemProjects.isSystemProjectName(""));
        assertFalse(SystemProjects.isSystemProjectName(null));

        // The allow-list is deliberately narrower than a bare "__" test, so a user may still name a project this way.
        assertFalse(SystemProjects.isSystemProjectName("__scratch"));
        assertFalse(SystemProjects.isSystemProjectName("__MY_PROJECT__"));
    }

    @Test
    void testIsSystemProjectReadsTheProjectName() {
        Project project = new Project();

        project.setName(SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX + 1);

        assertTrue(SystemProjects.isSystemProject(project));

        project.setName("My Project");

        assertFalse(SystemProjects.isSystemProject(project));
        assertFalse(SystemProjects.isSystemProject(null));
    }

    @Test
    void testProjectNameNotLikePredicatesContainsAnEscapeClausePerPrefix() {
        String predicates = SystemProjects.projectNameNotLikePredicates("project.name");

        assertTrue(predicates.contains("project.name NOT LIKE '\\_\\_AI\\_AGENT\\_\\_%' ESCAPE '\\'"));
        assertTrue(predicates.contains("project.name NOT LIKE '\\_\\_KNOWLEDGE\\_BASE\\_\\_%' ESCAPE '\\'"));
        assertTrue(predicates.contains("project.name NOT LIKE '\\_\\_CONTEXT\\_STORE\\_\\_%' ESCAPE '\\'"));
        assertTrue(
            predicates.contains("project.name NOT LIKE '\\_\\_EMBEDDED\\_AUTOMATION\\_\\_%' ESCAPE '\\'"));

        int escapeClauseCount = predicates.split("ESCAPE '\\\\'", -1).length - 1;

        assertTrue(
            escapeClauseCount == 4,
            "expected one ESCAPE clause per NAME_PREFIXES entry, got fragments: " + predicates);
    }

    @Test
    void testProjectNameNotLikePredicatesUsesTheGivenColumnReference() {
        String predicates = SystemProjects.projectNameNotLikePredicates("p.name");

        assertTrue(predicates.contains("p.name NOT LIKE"));
        assertFalse(predicates.contains("project.name NOT LIKE"));
    }

    /**
     * The escaping is the point of these helpers, not a detail of them. A prefix spliced in raw reads as a LIKE pattern
     * whose {@code _} characters are single-character wildcards, so {@code '__EMBEDDED__%'} matches
     * {@code 'MyEMBEDDEDxyz'} — which is how an ordinary project once disappeared from every non-embedded listing.
     */
    @Test
    void testConditionsEscapeTheUnderscoreWildcards() {
        assertEquals(
            "project.name NOT LIKE '\\_\\_EMBEDDED\\_\\_%' ESCAPE '\\' ",
            SystemProjects.notLikeCondition("project.name", SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX));
        assertEquals(
            "project.name LIKE '\\_\\_EMBEDDED\\_\\_%' ESCAPE '\\' ",
            SystemProjects.likeCondition("project.name", SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX));
    }

    /**
     * {@code notLikePredicate} is now {@code "AND " + notLikeCondition}; pinning that keeps the two from drifting into
     * two separately-maintained format strings.
     */
    @Test
    void testNotLikePredicateIsTheConditionWithAnAnd() {
        assertEquals(
            "AND " + SystemProjects.notLikeCondition("project.name", SystemProjects.AI_AGENT_NAME_PREFIX),
            SystemProjects.notLikePredicate("project.name", SystemProjects.AI_AGENT_NAME_PREFIX));
    }
}
