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

package com.bytechef.cli.command.embedded;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.cli.core.error.CliException;
import org.junit.jupiter.api.Test;

/**
 * Pins the embedded-configuration client's {@code toCliException} status-code mapping, which mirrors
 * {@code AutomationClientFactory}'s -- including the 404 branch that was once missing on the separate admin client's
 * overload, before that client was folded into this one.
 *
 * @author Ivica Cardic
 */
class EmbeddedConfigurationClientFactoryTest {

    @Test
    void testToCliExceptionMapsUnauthorizedToExitCode2() {
        CliException exception = EmbeddedConfigurationClientFactory.toCliException(
            new com.bytechef.cli.client.embeddedconfiguration.ApiException(401, "Unauthorized"));

        assertEquals(2, exception.exitCode());
    }

    @Test
    void testToCliExceptionMapsForbiddenToExitCode2() {
        CliException exception = EmbeddedConfigurationClientFactory.toCliException(
            new com.bytechef.cli.client.embeddedconfiguration.ApiException(403, "Forbidden"));

        assertEquals(2, exception.exitCode());
    }

    @Test
    void testToCliExceptionMapsNotFoundToExitCode3() {
        CliException exception = EmbeddedConfigurationClientFactory.toCliException(
            new com.bytechef.cli.client.embeddedconfiguration.ApiException(404, "Not Found"));

        assertEquals(3, exception.exitCode());
    }

    @Test
    void testToCliExceptionMapsOtherStatusToExitCode1() {
        CliException exception = EmbeddedConfigurationClientFactory.toCliException(
            new com.bytechef.cli.client.embeddedconfiguration.ApiException(500, "Server Error"));

        assertEquals(1, exception.exitCode());
    }
}
