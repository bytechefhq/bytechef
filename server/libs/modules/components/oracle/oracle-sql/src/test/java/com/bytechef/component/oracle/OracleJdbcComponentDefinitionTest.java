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

import com.bytechef.platform.component.jdbc.handler.JdbcComponentHandlerImpl;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
public class OracleJdbcComponentDefinitionTest {

    @Test
    public void testGetComponentDefinition() {
        JdbcComponentHandlerImpl jdbcComponentHandler = new JdbcComponentHandlerImpl(
            new OracleJdbcComponentHandler().getJdbcComponentDefinition());

        JsonFileAssert.assertEquals("definition/oracle_v1.json", jdbcComponentHandler.getDefinition());
    }
}
