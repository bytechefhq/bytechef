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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.Resources;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentDsl {

    private static final int VERSION_1 = 1;

    public static ModifiableJdbcComponentDefinition jdbcComponent(String name) {
        return new ModifiableJdbcComponentDefinition(name);
    }

    public static final class ModifiableJdbcComponentDefinition implements JdbcComponentDefinition {

        private String databaseJdbcName;
        private String icon;
        private String jdbcDriverClassName;
        private String description;
        private final String name;
        private Resources resources;
        private String title;
        private int version = VERSION_1;

        private ModifiableJdbcComponentDefinition(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public ModifiableJdbcComponentDefinition databaseJdbcName(String databaseJdbcName) {
            this.databaseJdbcName = databaseJdbcName;

            return this;
        }

        public ModifiableJdbcComponentDefinition jdbcDriverClassName(String jdbcDriverClassName) {
            this.jdbcDriverClassName = jdbcDriverClassName;

            return this;
        }

        public ModifiableJdbcComponentDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableJdbcComponentDefinition icon(String icon) {
            this.icon = icon;

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(String documentationUrl) {
            this.resources = new ResourcesImpl(documentationUrl, null);

            return this;
        }

        public ModifiableJdbcComponentDefinition resources(
            String documentationUrl, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(documentationUrl, additionalUrls);

            return this;
        }

        public ModifiableJdbcComponentDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableJdbcComponentDefinition version(int version) {
            this.version = version;

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ModifiableJdbcComponentDefinition that)) {
                return false;
            }

            return version == that.version && Objects.equals(databaseJdbcName, that.databaseJdbcName)
                && Objects.equals(icon, that.icon) && Objects.equals(jdbcDriverClassName, that.jdbcDriverClassName)
                && Objects.equals(description, that.description) && Objects.equals(name, that.name)
                && Objects.equals(resources, that.resources) && Objects.equals(title, that.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(databaseJdbcName, icon, jdbcDriverClassName, description, name, resources, title,
                version);
        }

        @Override
        public String getDatabaseJdbcName() {
            return databaseJdbcName;
        }

        @Override
        public String getJdbcDriverClassName() {
            return Objects.requireNonNull(jdbcDriverClassName);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.ofNullable(icon);
        }

        @Override
        public Optional<Resources> getResources() {
            return Optional.ofNullable(resources);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    private record ResourcesImpl(String documentationUrl, Map<String, String> additionalUrls) implements Resources {

        @Override
        public Optional<Map<String, String>> getAdditionalUrls() {
            return Optional.ofNullable(additionalUrls == null ? null : new HashMap<>(additionalUrls));
        }
    }
}
