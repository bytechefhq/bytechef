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

package com.bytechef.platform.component.jdbc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Property.Type;
import java.sql.Types;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JdbcComponentHandlerImplQueryOutputTest {

    @Test
    void toColumnPropertyMapsJdbcTypesToPropertyTypes() {
        assertEquals(Type.BOOLEAN, JdbcComponentHandlerImpl.toColumnProperty("active", Types.BOOLEAN)
            .getType());
        assertEquals(Type.INTEGER, JdbcComponentHandlerImpl.toColumnProperty("id", Types.BIGINT)
            .getType());
        assertEquals(Type.NUMBER, JdbcComponentHandlerImpl.toColumnProperty("price", Types.NUMERIC)
            .getType());
        assertEquals(Type.DATE, JdbcComponentHandlerImpl.toColumnProperty("born", Types.DATE)
            .getType());
        assertEquals(Type.TIME, JdbcComponentHandlerImpl.toColumnProperty("at", Types.TIME)
            .getType());
        assertEquals(Type.DATE_TIME, JdbcComponentHandlerImpl.toColumnProperty("created", Types.TIMESTAMP)
            .getType());
        assertEquals(Type.ARRAY, JdbcComponentHandlerImpl.toColumnProperty("tags", Types.ARRAY)
            .getType());
        assertEquals(Type.STRING, JdbcComponentHandlerImpl.toColumnProperty("email", Types.VARCHAR)
            .getType());
        assertEquals(Type.STRING, JdbcComponentHandlerImpl.toColumnProperty("payload", Types.OTHER)
            .getType());
    }
}
