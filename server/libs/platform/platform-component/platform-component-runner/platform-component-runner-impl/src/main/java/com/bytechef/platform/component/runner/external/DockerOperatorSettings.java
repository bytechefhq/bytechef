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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.runner.TaskRunnerOperatorFlag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class DockerOperatorSettings {

    public static final String ALLOWED_IMAGES_PROPERTY = "allowed-images";

    public static final String ALLOW_HOST_NETWORK_PROPERTY = "allow-host-network";

    public static final String ALLOWED_NETWORK_MODES_PROPERTY = "allowed-network-modes";

    public static final String DEFAULT_CPU_PROPERTY = "default-cpu";

    public static final String MAX_CPU_PROPERTY = "max-cpu";

    public static final String DEFAULT_MEMORY_PROPERTY = "default-memory";

    public static final String MAX_MEMORY_PROPERTY = "max-memory";

    public static final String PIDS_LIMIT_PROPERTY = "pids-limit";

    static final List<String> DEFAULT_ALLOWED_NETWORK_MODES = List.of("none", "bridge", "host");

    static final String DEFAULT_CPU = "1";

    static final String DEFAULT_MAX_CPU = "4";

    static final String DEFAULT_MEMORY = "512m";

    static final String DEFAULT_MAX_MEMORY = "2g";

    static final String DEFAULT_PIDS_LIMIT = "512";

    private static final String RUNNER_TYPE = "docker";
    private static final String HOST_PROPERTY = "host";
    private static final String ALLOW_VOLUME_MOUNTS_PROPERTY = "allow-volume-mounts";

    private final String host;
    private final List<String> allowedImages;
    private final List<String> allowedNetworkModes;
    private final boolean hostNetworkAllowed;
    private final boolean volumeMountAllowed;
    private final String defaultCpu;
    private final String maxCpu;
    private final String defaultMemory;
    private final String maxMemory;
    private final String pidsLimit;

    private DockerOperatorSettings(
        String host, List<String> allowedImages, List<String> allowedNetworkModes, boolean hostNetworkAllowed,
        boolean volumeMountAllowed, String defaultCpu, String maxCpu, String defaultMemory, String maxMemory,
        String pidsLimit) {

        this.host = host;
        this.allowedImages = allowedImages;
        this.allowedNetworkModes = allowedNetworkModes;
        this.hostNetworkAllowed = hostNetworkAllowed;
        this.volumeMountAllowed = volumeMountAllowed;
        this.defaultCpu = defaultCpu;
        this.maxCpu = maxCpu;
        this.defaultMemory = defaultMemory;
        this.maxMemory = maxMemory;
        this.pidsLimit = pidsLimit;
    }

    public static DockerOperatorSettings of(ApplicationProperties applicationProperties) {
        Map<String, String> properties = propertiesOf(applicationProperties);

        return new DockerOperatorSettings(
            properties.getOrDefault(HOST_PROPERTY, ""),
            parseList(properties.get(ALLOWED_IMAGES_PROPERTY), List.of()),
            parseList(properties.get(ALLOWED_NETWORK_MODES_PROPERTY), DEFAULT_ALLOWED_NETWORK_MODES),
            TaskRunnerOperatorFlag.isEnabled(applicationProperties, RUNNER_TYPE, ALLOW_HOST_NETWORK_PROPERTY),
            TaskRunnerOperatorFlag.isEnabled(applicationProperties, RUNNER_TYPE, ALLOW_VOLUME_MOUNTS_PROPERTY),
            valueOrDefault(properties.get(DEFAULT_CPU_PROPERTY), DEFAULT_CPU),
            valueOrDefault(properties.get(MAX_CPU_PROPERTY), DEFAULT_MAX_CPU),
            valueOrDefault(properties.get(DEFAULT_MEMORY_PROPERTY), DEFAULT_MEMORY),
            valueOrDefault(properties.get(MAX_MEMORY_PROPERTY), DEFAULT_MAX_MEMORY),
            valueOrDefault(properties.get(PIDS_LIMIT_PROPERTY), DEFAULT_PIDS_LIMIT));
    }

    public List<String> getAllowedImages() {
        return List.copyOf(allowedImages);
    }

    public List<String> getAllowedNetworkModes() {
        return List.copyOf(allowedNetworkModes);
    }

    public String getDefaultCpu() {
        return defaultCpu;
    }

    public String getDefaultMemory() {
        return defaultMemory;
    }

    public String getHost() {
        return host;
    }

    public String getMaxCpu() {
        return maxCpu;
    }

    public String getMaxMemory() {
        return maxMemory;
    }

    public String getPidsLimit() {
        return pidsLimit;
    }

    public boolean isNetworkModeAllowed(String networkMode) {
        return allowedNetworkModes.contains(networkMode);
    }

    public boolean isHostNetworkAllowed() {
        return hostNetworkAllowed;
    }

    public boolean isImageAllowed(String image) {
        return allowedImages.contains(image);
    }

    public boolean isVolumeMountAllowed() {
        return volumeMountAllowed;
    }

    private static List<String> parseList(String value, List<String> defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .toList();
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private static Map<String, String> propertiesOf(ApplicationProperties applicationProperties) {
        ApplicationProperties.Script script = applicationProperties.getScript();

        Map<String, ApplicationProperties.Script.Runner> runners = script.getRunners();

        if (runners == null) {
            return Map.of();
        }

        ApplicationProperties.Script.Runner runner = runners.get(RUNNER_TYPE);

        if (runner == null) {
            return Map.of();
        }

        Map<String, String> properties = runner.getProperties();

        if (properties == null) {
            return Map.of();
        }

        return properties;
    }
}
