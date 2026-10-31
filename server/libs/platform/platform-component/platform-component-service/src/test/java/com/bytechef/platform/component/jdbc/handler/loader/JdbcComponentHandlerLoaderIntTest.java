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

package com.bytechef.platform.component.jdbc.handler.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.handler.JdbcComponentHandlerImpl;
import com.bytechef.platform.component.jdbc.operation.config.JdbcOperationIntTestConfiguration;
import com.bytechef.platform.component.task.handler.ComponentTaskHandler;
import com.bytechef.platform.workflow.worker.security.JobPrincipalAuthenticationRunner;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = JdbcOperationIntTestConfiguration.class)
class JdbcComponentHandlerLoaderIntTest {

    @Autowired
    private JdbcConnectionDetails jdbcConnectionDetails;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final JdbcComponentHandlerLoader jdbcComponentHandlerLoader = new JdbcComponentHandlerLoader();

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test;");
        jdbcTemplate.execute(
            """
                    CREATE TABLE test (
                        id   varchar(256) not null primary key,
                        name varchar(256) not null
                    );
                    INSERT INTO test VALUES('id1', 'name1');
                    INSERT INTO test VALUES('id2', 'name2');
                """);
    }

    @Test
    void testLoadComponentHandlersDiscoversJdbcComponentsOnTheClasspath() {
        ComponentHandlerEntry componentHandlerEntry = getPostgreSQLComponentHandlerEntry();

        assertInstanceOf(JdbcComponentHandlerImpl.class, componentHandlerEntry.componentHandler());

        ComponentDefinition componentDefinition = componentHandlerEntry.componentHandler()
            .getDefinition();

        assertEquals("postgresql", componentDefinition.getName());
        assertEquals("PostgreSQL", componentDefinition.getTitle()
            .orElseThrow());
        assertEquals(
            List.of("query", "insert", "update", "delete", "execute"),
            componentDefinition.getActions()
                .orElseThrow()
                .stream()
                .map(ActionDefinition::getName)
                .toList());
    }

    @Test
    void testLoadComponentHandlersCreatesComponentTaskHandlers() {
        ComponentHandlerEntry componentHandlerEntry = getPostgreSQLComponentHandlerEntry();

        assertInstanceOf(
            ComponentTaskHandler.class,
            componentHandlerEntry.componentTaskHandlerFunction()
                .apply("query", mock(ActionDefinitionFacade.class), new JobPrincipalAuthenticationRunner(List.of())));
    }

    @Test
    void testLoadComponentHandlersQueryActionReadsFromTheDatabase() throws Exception {
        ComponentDefinition componentDefinition = getPostgreSQLComponentHandlerEntry().componentHandler()
            .getDefinition();

        ActionDefinition queryActionDefinition = componentDefinition.getActions()
            .orElseThrow()
            .stream()
            .filter(actionDefinition -> JdbcConstants.QUERY.equals(actionDefinition.getName()))
            .findFirst()
            .orElseThrow();

        PerformFunction performFunction = (PerformFunction) queryActionDefinition.getPerform()
            .orElseThrow();

        URI uri = URI.create(StringUtils.removeStart(jdbcConnectionDetails.getJdbcUrl(), "jdbc:"));

        Map<String, Object> inputParameters = Map.of(
            JdbcConstants.PARAMETERS, Map.of("id", "id2"),
            JdbcConstants.QUERY, "SELECT id, name FROM test WHERE id = :id");
        Map<String, Object> connectionParameters = Map.of(
            JdbcConstants.DATABASE, StringUtils.removeStart(uri.getPath(), "/"),
            JdbcConstants.HOST, uri.getHost(),
            JdbcConstants.PASSWORD, jdbcConnectionDetails.getPassword(),
            JdbcConstants.PORT, uri.getPort(),
            JdbcConstants.USERNAME, jdbcConnectionDetails.getUsername());

        Object result = performFunction.apply(
            ParametersFactory.create(inputParameters), ParametersFactory.create(connectionParameters),
            mock(ActionContext.class));

        assertEquals(List.of(Map.of("id", "id2", "name", "name2")), result);
    }

    private ComponentHandlerEntry getPostgreSQLComponentHandlerEntry() {
        return jdbcComponentHandlerLoader.loadComponentHandlers()
            .stream()
            .filter(componentHandlerEntry -> "postgresql".equals(
                componentHandlerEntry.componentHandler()
                    .getName()))
            .findFirst()
            .orElseThrow();
    }
}
