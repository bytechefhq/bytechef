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

package com.bytechef.automation.data.table.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.DeleteRowInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.ImportCsvInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.InsertRowInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.UpdateRowInput;
import com.bytechef.automation.data.table.web.graphql.config.AutomationDataTableGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Calls every data table row GraphQL operation of {@link DataTableRowGraphQlController} through the real Spring
 * method-security interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}, asserting each guard in both directions and the exact check that reaches
 * {@link PermissionService}. The guards sit on the controllers rather than on {@code DataTableService} and
 * {@code DataTableRowService}, because the data table component calls those services at workflow runtime, where no user
 * is present to hold a scope.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationDataTableGraphQlTestConfiguration.class,
    DataTableRowGraphQlController.class,
    DataTableRowGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = DataTableRowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/"
    })
class DataTableRowGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final long ROW_ID = 99L;
    private static final long TABLE_ID = 9L;

    @Autowired
    private DataTableRowGraphQlController dataTableRowGraphQlController;

    @MockitoBean
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

        when(expectedCheck.apply(permissionService)).thenReturn(granted);

        Method method = findMethod(guardCase.methodName());
        Object[] arguments = toArguments(method, guardCase.argumentsByName());

        if (granted) {
            assertThatThrownBy(() -> invoke(method, dataTableRowGraphQlController, arguments))
                .as("%s must reach its body when its permission check grants", guardCase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> invoke(method, dataTableRowGraphQlController, arguments))
                .as("%s must be denied when its permission check refuses", guardCase)
                .isInstanceOf(AccessDeniedException.class);
        }

        expectedCheck.apply(verify(permissionService));
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = Arrays.stream(DataTableRowGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::getName)
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::methodName)
            .collect(Collectors.toSet());

        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        Map<String, Object> tableArguments = Map.of("environmentId", ENVIRONMENT_ID, "tableId", TABLE_ID);

        return Stream.of(
            new GuardCase("dataTableRows", tableArguments, tableCheck("DATA_TABLE_VIEW")),
            new GuardCase("dataTableRowsPage", tableArguments, tableCheck("DATA_TABLE_VIEW")),
            new GuardCase("exportDataTableCsv", tableArguments, tableCheck("DATA_TABLE_VIEW")),
            new GuardCase(
                "insertDataTableRow",
                Map.of("input", new InsertRowInput(ENVIRONMENT_ID, TABLE_ID, Map.of())),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "updateDataTableRow",
                Map.of("input", new UpdateRowInput(ENVIRONMENT_ID, TABLE_ID, ROW_ID, Map.of())),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "deleteDataTableRow",
                Map.of("input", new DeleteRowInput(ENVIRONMENT_ID, TABLE_ID, ROW_ID)), tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "importDataTableCsv",
                Map.of("input", new ImportCsvInput(ENVIRONMENT_ID, TABLE_ID, "total\n1")),
                tableCheck("DATA_TABLE_EDIT")));
    }

    private static Function<PermissionService, Boolean> tableCheck(String scope) {
        return permissionService -> permissionService.hasResourceScopeInEnvironment(
            TABLE_ID, "DataTable", scope, PRODUCTION);
    }

    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static Method findMethod(String methodName) {
        List<Method> methods = Arrays.stream(DataTableRowGraphQlController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()))
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(methods)
            .as("Expected exactly one public '%s' method, since a proxy only enforces a public guard", methodName)
            .hasSize(1);

        return methods.getFirst();
    }

    private static Object[] toArguments(Method method, Map<String, Object> argumentsByName) {
        Parameter[] parameters = method.getParameters();

        List<String> parameterNames = Arrays.stream(parameters)
            .map(Parameter::getName)
            .toList();

        assertThat(parameterNames)
            .as("%s must declare every parameter its guard is evaluated with", method.getName())
            .containsAll(argumentsByName.keySet());

        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++) {
            arguments[index] = argumentsByName.get(parameterNames.get(index));
        }

        return arguments;
    }

    private record GuardCase(
        String methodName, Map<String, Object> argumentsByName, Function<PermissionService, Boolean> expectedCheck) {

        @Override
        public String toString() {
            return methodName;
        }
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            return expressionHandler;
        }

        @Bean
        DataTableRowService dataTableRowService() {
            return bodyReachedMock(DataTableRowService.class);
        }

        @Bean
        DataTableService dataTableService() {
            return bodyReachedMock(DataTableService.class);
        }

        private static <T> T bodyReachedMock(Class<T> type) {
            return mock(type, invocation -> {
                throw new IllegalStateException(BODY_REACHED);
            });
        }
    }
}
