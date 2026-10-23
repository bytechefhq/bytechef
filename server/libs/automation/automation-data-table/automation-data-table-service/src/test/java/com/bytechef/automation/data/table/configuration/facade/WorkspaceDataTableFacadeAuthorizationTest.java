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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls every guarded {@link WorkspaceDataTableFacadeImpl} method through the real method-security interceptor, backed
 * by the production expression handler and permission evaluator, and pins the one permission check each guard makes.
 *
 * @author Ivica Cardic
 */
class WorkspaceDataTableFacadeAuthorizationTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final long ROW_ID = 99L;
    private static final long TABLE_ID = 9L;
    private static final long WORKSPACE_ID = 42L;

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
        PermissionService permissionService = mock(PermissionService.class);
        Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

        when(expectedCheck.apply(permissionService)).thenReturn(granted);

        Method method = findMethod(guardCase.methodName());
        Object[] arguments = toArguments(method, guardCase.argumentsByName());
        WorkspaceDataTableFacade workspaceDataTableFacade = secure(
            new WorkspaceDataTableFacadeImpl(
                bodyReachedMock(DataTableRowService.class), bodyReachedMock(DataTableService.class),
                bodyReachedMock(DataTableTagService.class), bodyReachedMock(DataTableWebhookService.class),
                bodyReachedMock(WorkspaceDataTableService.class)),
            permissionService);

        if (granted) {
            assertThatThrownBy(() -> invoke(method, workspaceDataTableFacade, arguments))
                .as("%s must reach its body when its permission check grants", guardCase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> invoke(method, workspaceDataTableFacade, arguments))
                .as("%s must be denied when its permission check refuses", guardCase)
                .isInstanceOf(AccessDeniedException.class);
        }

        expectedCheck.apply(verify(permissionService));
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = Arrays.stream(WorkspaceDataTableFacadeImpl.class.getDeclaredMethods())
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
        Map<String, Object> workspaceArguments = Map.of("environmentId", ENVIRONMENT_ID, "workspaceId", WORKSPACE_ID);

        return Stream.of(
            new GuardCase(
                "createTable",
                Map.of(
                    "baseName", "orders", "columnSpecs", List.of(), "description", "Orders", "environmentId",
                    ENVIRONMENT_ID, "workspaceId", WORKSPACE_ID),
                workspaceCheck("DATA_TABLE_CREATE")),
            new GuardCase("listTables", workspaceArguments, workspaceCheck("DATA_TABLE_VIEW")),
            new GuardCase("getTagsByTableId", workspaceArguments, workspaceCheck("DATA_TABLE_VIEW")),
            new GuardCase(
                "addColumn", Map.of("environmentId", ENVIRONMENT_ID, "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase("dropTable", tableArguments, tableCheck("DATA_TABLE_DELETE")),
            new GuardCase(
                "duplicateTable",
                Map.of("environmentId", ENVIRONMENT_ID, "newBaseName", "orders_copy", "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_CREATE")),
            new GuardCase(
                "removeColumn",
                Map.of("columnName", "column_1", "environmentId", ENVIRONMENT_ID, "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "renameColumn",
                Map.of(
                    "environmentId", ENVIRONMENT_ID, "fromColumnName", "column_1", "tableId", TABLE_ID,
                    "toColumnName", "total"),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "renameTable",
                Map.of("environmentId", ENVIRONMENT_ID, "newBaseName", "invoices", "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "listRows",
                Map.of("environmentId", ENVIRONMENT_ID, "limit", 10, "offset", 0, "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_VIEW")),
            new GuardCase("exportCsv", tableArguments, tableCheck("DATA_TABLE_VIEW")),
            new GuardCase("listWebhooks", tableArguments, tableCheck("DATA_TABLE_VIEW")),
            new GuardCase(
                "insertRow", Map.of("environmentId", ENVIRONMENT_ID, "tableId", TABLE_ID, "values", Map.of()),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "updateRow",
                Map.of("environmentId", ENVIRONMENT_ID, "rowId", ROW_ID, "tableId", TABLE_ID, "values", Map.of()),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "deleteRow", Map.of("environmentId", ENVIRONMENT_ID, "rowId", ROW_ID, "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "importCsv", Map.of("csv", "total\n1", "environmentId", ENVIRONMENT_ID, "tableId", TABLE_ID),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                "updateTags", Map.of("tableId", TABLE_ID, "tags", List.of()),
                permissionService -> permissionService.hasResourceScope(TABLE_ID, "DataTable", "DATA_TABLE_EDIT")));
    }

    private static Function<PermissionService, Boolean> tableCheck(String scope) {
        return permissionService -> permissionService.hasResourceScopeInEnvironment(
            TABLE_ID, "DataTable", scope, PRODUCTION);
    }

    private static Function<PermissionService, Boolean> workspaceCheck(String scope) {
        return permissionService -> permissionService.hasWorkspaceScope(WORKSPACE_ID, scope, PRODUCTION);
    }

    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static <T> T bodyReachedMock(Class<T> type) {
        return mock(type, invocation -> {
            throw new IllegalStateException(BODY_REACHED);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(T target, PermissionService permissionService) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(
            preAuthorizeAuthorizationManager));

        return (T) proxyFactory.getProxy();
    }

    private static Method findMethod(String methodName) {
        List<Method> methods = Arrays.stream(WorkspaceDataTableFacadeImpl.class.getDeclaredMethods())
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
}
