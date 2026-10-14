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

package com.bytechef.platform.component.runner.external;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class DockerOperatorSettings {

    public static final String ALLOW_TASK_HOST_PROPERTY = "allow-task-host";
    public static final String ALLOW_VOLUME_MOUNTS_PROPERTY = "allow-volume-mounts";
    public static final String HOST_PROPERTY = "host";

    private final String host;
    private final boolean taskHostAllowed;
    private final boolean volumeMountAllowed;

    private DockerOperatorSettings(String host, boolean taskHostAllowed, boolean volumeMountAllowed) {
        this.host = host;
        this.taskHostAllowed = taskHostAllowed;
        this.volumeMountAllowed = volumeMountAllowed;
    }

    public static DockerOperatorSettings of(Map<String, String> properties) {
        String host = properties.get(HOST_PROPERTY);

        return new DockerOperatorSettings(
            host == null ? "" : host.trim(),
            Boolean.parseBoolean(properties.get(ALLOW_TASK_HOST_PROPERTY)),
            Boolean.parseBoolean(properties.get(ALLOW_VOLUME_MOUNTS_PROPERTY)));
    }

    public String getHost() {
        return host;
    }

    public boolean isTaskHostAllowed() {
        return taskHostAllowed;
    }

    public boolean isVolumeMountAllowed() {
        return volumeMountAllowed;
    }
}
