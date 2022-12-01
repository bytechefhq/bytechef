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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.constants.Versions;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Resources;

/**
 * @author Ivica Cardic
 */
public sealed class JdbcComponentDefinition permits ComponentDSL.ModifiableJdbcComponentDefinition {
    protected String databaseJdbcName;
    protected String jdbcDriverClassName;
    protected Display display;
    protected Resources resources;
    protected double version = Versions.VERSION_1;
    private final String name;

    protected JdbcComponentDefinition(String name) {
        this.name = name;
    }

    public String getDatabaseJdbcName() {
        return databaseJdbcName;
    }

    public String getJdbcDriverClassName() {
        return jdbcDriverClassName;
    }

    public Display getDisplay() {
        return display;
    }

    public Resources getResources() {
        return resources;
    }

    public String getName() {
        return name;
    }

    public double getVersion() {
        return version;
    }
}
