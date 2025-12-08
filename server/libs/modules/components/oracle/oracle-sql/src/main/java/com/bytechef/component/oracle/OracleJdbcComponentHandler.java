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

package com.bytechef.component.oracle;

import static com.bytechef.platform.component.definition.JdbcComponentDsl.jdbcComponent;

import com.bytechef.platform.component.JdbcComponentHandler;
import com.bytechef.platform.component.definition.JdbcComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Igor Beslic
 */
@AutoService(JdbcComponentHandler.class)
public class OracleJdbcComponentHandler implements JdbcComponentHandler {

    private static final JdbcComponentDefinition COMPONENT_DEFINITION = jdbcComponent("oracleSql")
        .title("Oracle SQL")
        .description("Select, insert, update and delete data from Oracle Databases.")
        .icon("path:assets/oracle.svg")
        .urlTemplate("jdbc:oracle:thin:@{host}:{port}:{database}")
        .jdbcDriverClassName("oracle.jdbc.OracleDriver");

    @Override
    public JdbcComponentDefinition getJdbcComponentDefinition() {
        return COMPONENT_DEFINITION;
    }
}
