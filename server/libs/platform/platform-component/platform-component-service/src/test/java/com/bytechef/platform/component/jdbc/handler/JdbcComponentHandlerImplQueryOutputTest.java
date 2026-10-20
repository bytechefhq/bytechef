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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.component.definition.Resources;
import com.bytechef.platform.component.definition.JdbcComponentDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class JdbcComponentHandlerImplQueryOutputTest {

    @Test
    void hasConnectionParametersRequiresHostAndDatabase() {
        assertTrue(JdbcComponentHandlerImpl.hasConnectionParameters(
            ParametersFactory.create(Map.of("host", "localhost", "database", "bytechef"))));
        assertFalse(JdbcComponentHandlerImpl.hasConnectionParameters(ParametersFactory.create(Map.of())));
        assertFalse(JdbcComponentHandlerImpl.hasConnectionParameters(
            ParametersFactory.create(Map.of("host", "localhost"))));
        assertFalse(JdbcComponentHandlerImpl.hasConnectionParameters(
            ParametersFactory.create(Map.of("host", " ", "database", "bytechef"))));
    }

    @Test
    void queryOutputReturnsNullWithoutConnectionParameters() throws Exception {
        JdbcComponentHandlerImpl jdbcComponentHandler = new JdbcComponentHandlerImpl(new JdbcComponentDefinition() {

            @Override
            public String getUrlTemplate() {
                return "jdbc:postgresql://{host}:{port}/{database}";
            }

            @Override
            public String getJdbcDriverClassName() {
                return "org.postgresql.Driver";
            }

            @Override
            public Optional<String> getDescription() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getIcon() {
                return Optional.empty();
            }

            @Override
            public Optional<Resources> getResources() {
                return Optional.empty();
            }

            @Override
            public String getName() {
                return "postgresql";
            }

            @Override
            public Optional<String> getTitle() {
                return Optional.empty();
            }

            @Override
            public int getVersion() {
                return 1;
            }
        });

        assertNull(
            jdbcComponentHandler.queryOutput(
                ParametersFactory.create(Map.of("query", "SELECT 1")), ParametersFactory.create(Map.of()),
                mock(ActionContext.class)));
    }

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
