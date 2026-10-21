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

package com.bytechef.cli.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads and writes the CLI configuration file, an INI-style file with one section per named profile.
 *
 * @author Ivica Cardic
 */
public final class ConfigFile {

    private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private ConfigFile() {
    }

    public static Map<String, Profile> read(Path file) throws IOException {
        Map<String, Profile> profiles = new LinkedHashMap<>();

        if (!Files.exists(file)) {
            return profiles;
        }

        List<String> lines = Files.readAllLines(file);

        String section = null;
        Map<String, String> values = new LinkedHashMap<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                if (section != null) {
                    profiles.put(section, toProfile(values));
                }

                section = line.substring(1, line.length() - 1)
                    .trim();
                values = new LinkedHashMap<>();
            } else {
                int equalsIndex = line.indexOf('=');

                if (equalsIndex > 0 && section != null) {
                    values.put(
                        line.substring(0, equalsIndex)
                            .trim(),
                        line.substring(equalsIndex + 1)
                            .trim());
                }
            }
        }

        if (section != null) {
            profiles.put(section, toProfile(values));
        }

        return profiles;
    }

    public static void write(Path file, String profileName, Profile profile) throws IOException {
        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Map<String, Profile> profiles = read(file);

        profiles.put(profileName, profile);

        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, Profile> entry : profiles.entrySet()) {
            Profile entryProfile = entry.getValue();

            stringBuilder.append('[')
                .append(entry.getKey())
                .append("]\n");
            stringBuilder.append("host = ")
                .append(entryProfile.host())
                .append('\n');
            stringBuilder.append("token = ")
                .append(entryProfile.token())
                .append('\n');
            stringBuilder.append("environment = ")
                .append(entryProfile.environment())
                .append('\n');

            if (entryProfile.workspaceId() != null) {
                stringBuilder.append("workspace_id = ")
                    .append(entryProfile.workspaceId())
                    .append('\n');
            }

            stringBuilder.append('\n');
        }

        // Create the file with owner-only permissions BEFORE writing the token, so the secret is
        // never briefly readable by group/other during the write.
        if (!Files.exists(file)) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        }

        Files.setPosixFilePermissions(file, OWNER_ONLY);
        Files.writeString(file, stringBuilder.toString());
    }

    private static Profile toProfile(Map<String, String> values) {
        String workspaceId = values.get("workspace_id");
        String environment = values.get("environment");

        return new Profile(
            values.get("host"),
            values.get("token"),
            environment == null ? null : Environment.valueOf(environment),
            workspaceId == null ? null : Long.valueOf(workspaceId));
    }
}
