/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.postgresql;

import static com.bytechef.component.definition.ComponentDSL.jdbcComponent;
import static com.bytechef.component.postgresql.constant.PostgreSQLConstants.POSTGRESQL;

import com.bytechef.component.JdbcComponentDefinitionFactory;
import com.bytechef.component.definition.JdbcComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(JdbcComponentDefinitionFactory.class)
public class PostgreSQLJdbcComponentDefinitionFactory implements JdbcComponentDefinitionFactory {

    private static final JdbcComponentDefinition COMPONENT_DEFINITION = jdbcComponent(POSTGRESQL)
        .title("PostgreSQL")
        .description("Query, insert and update data from PostgreSQL.")
        .icon("path:assets/postgresql.svg")
        .databaseJdbcName("postgresql")
        .jdbcDriverClassName("org.postgresql.Driver");

    @Override
    public JdbcComponentDefinition getJdbcComponentDefinition() {
        return COMPONENT_DEFINITION;
    }
}
