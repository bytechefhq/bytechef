/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.jdbc;

import com.bytechef.hermes.component.constants.Versions;
import com.bytechef.hermes.component.jdbc.definition.JdbcComponentDefinition;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Resources;

public final class JdbcComponentDSL {

    public static JdbcComponentDefinitionImpl createJdbcComponent(String name) {
        return new JdbcComponentDefinitionImpl(name);
    }

    public static final class JdbcComponentDefinitionImpl implements JdbcComponentDefinition {
        private String databaseJdbcName;
        private String jdbcDriverClassName;
        private Display display;
        private Resources resources;
        private String name;
        private double version = Versions.VERSION_1;

        public JdbcComponentDefinitionImpl(String name) {
            this.name = name;
        }

        public JdbcComponentDefinitionImpl databaseJdbcName(String databaseJdbcName) {
            this.databaseJdbcName = databaseJdbcName;

            return this;
        }

        public JdbcComponentDefinitionImpl jdbcDriverClassName(String jdbcDriverClassName) {
            this.jdbcDriverClassName = jdbcDriverClassName;

            return this;
        }

        public JdbcComponentDefinitionImpl display(Display display) {
            this.display = display;

            return this;
        }

        public JdbcComponentDefinitionImpl resources(Resources resources) {
            this.resources = resources;

            return this;
        }

        public JdbcComponentDefinitionImpl version(double version) {
            this.version = version;

            return this;
        }

        @Override
        public String getDatabaseJdbcName() {
            return databaseJdbcName;
        }

        @Override
        public String getJdbcDriverClassName() {
            return jdbcDriverClassName;
        }

        @Override
        public Display getDisplay() {
            return display;
        }

        @Override
        public Resources getResources() {
            return resources;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double getVersion() {
            return version;
        }
    }
}
