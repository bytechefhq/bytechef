/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.licence.LicenceStatus;
import graphql.GraphqlErrorException;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LicenceEnforcementInstrumentation}.
 *
 * <p>
 * Testing approach: the real {@code beginExecuteOperation} hook is invoked directly. Graphql-java's {@link Parser}
 * builds a genuine {@link OperationDefinition} from a query string; {@link ExecutionContext} and
 * {@link InstrumentationExecuteOperationParameters} are mocked with Mockito so that {@code getOperationDefinition()}
 * returns the parsed operation without requiring a live {@code GraphQL} execution pipeline.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class LicenceEnforcementInstrumentationTest {

    private EeGraphQlFieldRegistry eeGraphQlFieldRegistry;
    private LicenceManager licenceManager;
    private LicenceEnforcementInstrumentation instrumentation;

    @BeforeEach
    void setUp() {
        eeGraphQlFieldRegistry = mock(EeGraphQlFieldRegistry.class);
        licenceManager = mock(LicenceManager.class);
        instrumentation = new LicenceEnforcementInstrumentation(eeGraphQlFieldRegistry, licenceManager);
    }

    @Test
    void testEeFieldWithInactiveLicenceThrows() {
        InstrumentationExecuteOperationParameters parameters = parametersForQuery("query { someEeFeature { id } }");

        when(eeGraphQlFieldRegistry.isEeField("someEeFeature")).thenReturn(true);
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);

        assertThatThrownBy(() -> instrumentation.beginExecuteOperation(parameters, null))
            .isInstanceOf(GraphqlErrorException.class)
            .satisfies(throwable -> {
                GraphqlErrorException graphqlErrorException = (GraphqlErrorException) throwable;

                assertThat(graphqlErrorException.getExtensions()).containsEntry("errorKey", "LICENCE_REQUIRED");
            });
    }

    @Test
    void testCeFieldWithInactiveLicenceDoesNotThrow() {
        InstrumentationExecuteOperationParameters parameters = parametersForQuery("query { someField { id } }");

        when(eeGraphQlFieldRegistry.isEeField("someField")).thenReturn(false);
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);

        assertThatCode(() -> instrumentation.beginExecuteOperation(parameters, null))
            .doesNotThrowAnyException();
    }

    @Test
    void testEeFieldWithActiveLicenceDoesNotThrow() {
        InstrumentationExecuteOperationParameters parameters = parametersForQuery("query { someEeFeature { id } }");

        when(eeGraphQlFieldRegistry.isEeField("someEeFeature")).thenReturn(true);
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.VALID);

        assertThatCode(() -> instrumentation.beginExecuteOperation(parameters, null))
            .doesNotThrowAnyException();
    }

    @Test
    void testIntrospectionFieldWithInactiveLicenceDoesNotThrow() {
        InstrumentationExecuteOperationParameters parameters =
            parametersForQuery("query { __schema { types { name } } }");

        when(eeGraphQlFieldRegistry.isEeField("__schema")).thenReturn(false);
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);

        assertThatCode(() -> instrumentation.beginExecuteOperation(parameters, null))
            .doesNotThrowAnyException();
    }

    @Test
    void testLicenceManagementFieldNotBlockedWithInactiveLicence() {
        for (String exemptField : LicenceEnforcementInstrumentation.LICENCE_MANAGEMENT_FIELDS) {
            InstrumentationExecuteOperationParameters parameters =
                parametersForQuery("query { " + exemptField + " { id } }");

            when(eeGraphQlFieldRegistry.isEeField(exemptField)).thenReturn(true);
            when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);

            assertThatCode(() -> instrumentation.beginExecuteOperation(parameters, null))
                .as("Exempt licence-management field '%s' must not be blocked even when status is MISSING", exemptField)
                .doesNotThrowAnyException();
        }
    }

    @Test
    void testNullSelectionSetDoesNotThrow() {
        OperationDefinition operationDefinitionWithNullSelectionSet = OperationDefinition.newOperationDefinition()
            .operation(OperationDefinition.Operation.QUERY)
            .build();

        ExecutionContext executionContext = mock(ExecutionContext.class);

        when(executionContext.getOperationDefinition()).thenReturn(operationDefinitionWithNullSelectionSet);

        InstrumentationExecuteOperationParameters parameters = mock(InstrumentationExecuteOperationParameters.class);

        when(parameters.getExecutionContext()).thenReturn(executionContext);

        assertThatCode(() -> instrumentation.beginExecuteOperation(parameters, null))
            .doesNotThrowAnyException();
    }

    private static InstrumentationExecuteOperationParameters parametersForQuery(String queryString) {
        Document document = new Parser().parseDocument(queryString);
        OperationDefinition operationDefinition = (OperationDefinition) document.getDefinitions()
            .getFirst();

        ExecutionContext executionContext = mock(ExecutionContext.class);

        when(executionContext.getOperationDefinition()).thenReturn(operationDefinition);

        InstrumentationExecuteOperationParameters parameters = mock(InstrumentationExecuteOperationParameters.class);

        when(parameters.getExecutionContext()).thenReturn(executionContext);

        return parameters;
    }
}
