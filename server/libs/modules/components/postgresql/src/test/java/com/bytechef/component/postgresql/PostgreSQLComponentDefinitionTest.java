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

import com.bytechef.platform.component.jdbc.handler.JdbcComponentHandler;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class PostgreSQLComponentDefinitionTest {

    @Test
    public void testGetComponentDefinition() {
        JdbcComponentHandler jdbcComponentHandler = new JdbcComponentHandler(
            new PostgreSQLJdbcComponentHandler().getJdbcComponentDefinition());

        JsonFileAssert.assertEquals("definition/postgresql_v1.json", jdbcComponentHandler.getDefinition());
    }
}
