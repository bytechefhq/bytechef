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

package com.bytechef.platform.configuration.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close editor-job-log IDOR (T24). Editor test-run log reads resolve
 * the owning workspace via the {@code Job:ResourceRole} token (test job &rarr; workflow &rarr; project &rarr;
 * workspace).
 *
 * @author Ivica Cardic
 */
class EditorLogFileStorageReaderAuthorizationTest {

    @Test
    void testReadLogEntriesRequiresJobViewer() {
        assertExpression("readLogEntries");
    }

    @Test
    void testReadLogEntriesByJobIdRequiresJobViewer() {
        assertExpression("readLogEntriesByJobId");
    }

    @Test
    void testLogsExistRequiresJobViewer() {
        assertExpression("logsExist");
    }

    private static void assertExpression(String methodName) {
        Method match = null;

        for (Method candidate : EditorLogFileStorageReaderImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = match.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission(#jobId, 'Job', 'EXECUTION_VIEW')");
    }
}
