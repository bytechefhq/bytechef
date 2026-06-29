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

package com.bytechef.platform.component.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close job-log IDOR (T24). Log reads resolve the owning workspace via
 * the {@code Job:ResourceRole} token (job &rarr; workflow &rarr; project &rarr; workspace); reads require VIEWER, the
 * destructive log delete requires EDITOR. The async write path ({@code storeLogEntry}) is invoked by workers without a
 * {@code SecurityContext} and stays ungated.
 *
 * @author Ivica Cardic
 */
class LogFileStorageAuthorizationTest {

    @Test
    void testReadLogEntriesRequiresJobViewer() {
        assertExpression("readLogEntries", "hasPermission(#jobId, 'Job', 'EXECUTION_VIEW')");
    }

    @Test
    void testReadLogEntriesByJobIdRequiresJobViewer() {
        assertExpression("readLogEntriesByJobId", "hasPermission(#jobId, 'Job', 'EXECUTION_VIEW')");
    }

    @Test
    void testLogsExistRequiresJobViewer() {
        assertExpression("logsExist", "hasPermission(#jobId, 'Job', 'EXECUTION_VIEW')");
    }

    @Test
    void testDeleteLogEntriesRequiresJobEditor() {
        assertExpression("deleteLogEntries", "hasPermission(#jobId, 'Job', 'EXECUTION_DELETE')");
    }

    @Test
    void testStoreLogEntryIsNotGated() {
        Method match = findMethod("storeLogEntry");

        assertThat(match.isAnnotationPresent(PreAuthorize.class))
            .as("worker write path storeLogEntry must NOT carry @PreAuthorize")
            .isFalse();
    }

    private static void assertExpression(String methodName, String expression) {
        Method match = findMethod(methodName);

        PreAuthorize preAuthorize = match.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    private static Method findMethod(String methodName) {
        for (Method candidate : LogFileStorageImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {

                return candidate;
            }
        }

        throw new AssertionError("method " + methodName + " not found");
    }
}
