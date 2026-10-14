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

package com.bytechef.platform.component.runner;

import com.bytechef.config.ApplicationProperties;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class TaskRunnerOperatorFlag {

    private TaskRunnerOperatorFlag() {
    }

    public static boolean isEnabled(
        ApplicationProperties applicationProperties, String type, String propertyName) {

        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = script.getRunners();

        if (runners == null) {
            return false;
        }

        ApplicationProperties.Script.Runner runner = runners.get(type);

        if (runner == null) {
            return false;
        }

        Map<String, String> properties = runner.getProperties();

        if (properties == null) {
            return false;
        }

        return Boolean.parseBoolean(properties.get(propertyName));
    }

    public static String getPropertyName(String type, String propertyName) {
        return "bytechef.script.runners.%s.properties.%s".formatted(type, propertyName);
    }
}
