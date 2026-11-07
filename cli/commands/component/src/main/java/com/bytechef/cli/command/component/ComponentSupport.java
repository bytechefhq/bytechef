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

package com.bytechef.cli.command.component;

import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
final class ComponentSupport {

    static final String PLATFORM_API_PATH = "/api/platform/v1";

    private ComponentSupport() {
    }

    static CliConfig resolve(
        Path configPath, Map<String, String> environmentVariables, String profile, String host, String token,
        String environment) {

        return new ProfileResolver(configPath, environmentVariables).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), null, profile));
    }
}
