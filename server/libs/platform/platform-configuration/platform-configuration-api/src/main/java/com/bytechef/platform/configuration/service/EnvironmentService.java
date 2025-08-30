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

package com.bytechef.platform.configuration.service;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * Service for accessing platform environments.
 *
 * @author Ivica Cardic
 */
public interface EnvironmentService {

    /**
     * Returns all available environments.
     */
    List<Environment> getEnvironments();

    /**
     * Returns the environment by its name (case-insensitive).
     *
     * @throws IllegalArgumentException if no environment matches the provided name
     */
    default Environment getEnvironment(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Environment name must not be null");
        }

        for (Environment environment : Environment.values()) {
            String environmentName = environment.name();

            if (environmentName.equalsIgnoreCase(name)) {
                return environment;
            }
        }

        throw new IllegalArgumentException("Unknown environment name: " + name);
    }

    /**
     * Returns the environment by its numeric identifier (ordinal).
     *
     * @throws IllegalArgumentException if the identifier is out of range
     */
    default Environment getEnvironment(int environmentId) {
        Environment[] values = Environment.values();

        if (environmentId < 0 || environmentId >= values.length) {
            throw new IllegalArgumentException("Environment id out of range: " + environmentId);
        }

        return values[environmentId];
    }
}
