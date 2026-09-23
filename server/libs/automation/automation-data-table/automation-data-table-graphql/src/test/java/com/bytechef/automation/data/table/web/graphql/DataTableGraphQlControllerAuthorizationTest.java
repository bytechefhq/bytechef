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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.AddColumnInput;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.CreateDataTableInput;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.DuplicateDataTableInput;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.RemoveColumnInput;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.RemoveTableInput;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.RenameColumnInput;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.RenameDataTableInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.DeleteRowInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.ImportCsvInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.InsertRowInput;
import com.bytechef.automation.data.table.web.graphql.DataTableRowGraphQlController.UpdateRowInput;
import com.bytechef.automation.data.table.web.graphql.DataTableTagGraphQlController.UpdateDataTableTagsInput;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on every user-facing data table GraphQL operation through the
 * real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each
 * guard in both directions and the exact check that reaches {@link PermissionService}. The guards sit on the
 * controllers rather than on {@code DataTableService} and {@code DataTableRowService}, because the data table component
 * calls those services at workflow runtime, where no user is present to hold a scope.
 *
 * @author Ivica Cardic
 */
class DataTableGraphQlControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final List<Class<?>> GUARDED_CONTROLLER_CLASSES = List.of(
        DataTableGraphQlController.class, DataTableRowGraphQlController.class, DataTableTagGraphQlController.class,
        DataTableWebhookGraphQlController.class);
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final long ROW_ID = 99L;
    private static final long TABLE_ID = 9L;
    private static final long WORKSPACE_ID = 42L;

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);
        Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

        when(expectedCheck.apply(permissionService)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, guardCase))
            .as("%s must %s when its permission check returns %s", guardCase, granted ? "allow" : "deny", granted)
            .isEqualTo(granted);

        expectedCheck.apply(verify(permissionService));
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = GUARDED_CONTROLLER_CLASSES.stream()
            .flatMap(controllerClass -> Arrays.stream(controllerClass.getDeclaredMethods()))
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(method -> method.getDeclaringClass()
                .getSimpleName() + "#" + method.getName())
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::toString)
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
                DataTableGraphQlController.class, "createDataTable",
                Map.of(
                    "input",
                    new CreateDataTableInput(ENVIRONMENT_ID, "orders", "Orders", List.of(), WORKSPACE_ID)),
                permissionService -> permissionService.hasWorkspaceScope(
                    WORKSPACE_ID, "DATA_TABLE_CREATE", PRODUCTION)),
            new GuardCase(
                DataTableGraphQlController.class, "dataTables", workspaceArguments,
                workspaceViewCheck()),
            new GuardCase(
                DataTableGraphQlController.class, "addDataTableColumn",
                Map.of("input", new AddColumnInput(ENVIRONMENT_ID, TABLE_ID, null)), tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableGraphQlController.class, "dropDataTable",
                Map.of("input", new RemoveTableInput(ENVIRONMENT_ID, TABLE_ID)), tableCheck("DATA_TABLE_DELETE")),
            new GuardCase(
                DataTableGraphQlController.class, "duplicateDataTable",
                Map.of("input", new DuplicateDataTableInput(ENVIRONMENT_ID, TABLE_ID, "orders_copy")),
                tableCheck("DATA_TABLE_CREATE")),
            new GuardCase(
                DataTableGraphQlController.class, "removeDataTableColumn",
                Map.of("input", new RemoveColumnInput(ENVIRONMENT_ID, TABLE_ID, "column_1")),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableGraphQlController.class, "renameDataTableColumn",
                Map.of("input", new RenameColumnInput(ENVIRONMENT_ID, TABLE_ID, "column_1", "total")),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableGraphQlController.class, "renameDataTable",
                Map.of("input", new RenameDataTableInput(ENVIRONMENT_ID, TABLE_ID, "invoices")),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableRowGraphQlController.class, "dataTableRows", tableArguments, tableCheck("DATA_TABLE_VIEW")),
            new GuardCase(
                DataTableRowGraphQlController.class, "dataTableRowsPage", tableArguments,
                tableCheck("DATA_TABLE_VIEW")),
            new GuardCase(
                DataTableRowGraphQlController.class, "exportDataTableCsv", tableArguments,
                tableCheck("DATA_TABLE_VIEW")),
            new GuardCase(
                DataTableRowGraphQlController.class, "insertDataTableRow",
                Map.of("input", new InsertRowInput(ENVIRONMENT_ID, TABLE_ID, Map.of())),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableRowGraphQlController.class, "updateDataTableRow",
                Map.of("input", new UpdateRowInput(ENVIRONMENT_ID, TABLE_ID, ROW_ID, Map.of())),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableRowGraphQlController.class, "deleteDataTableRow",
                Map.of("input", new DeleteRowInput(ENVIRONMENT_ID, TABLE_ID, ROW_ID)), tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableRowGraphQlController.class, "importDataTableCsv",
                Map.of("input", new ImportCsvInput(ENVIRONMENT_ID, TABLE_ID, "total\n1")),
                tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableTagGraphQlController.class, "dataTableTags", workspaceArguments, workspaceViewCheck()),
            new GuardCase(
                DataTableTagGraphQlController.class, "dataTableTagsByTable", workspaceArguments,
                workspaceViewCheck()),
            new GuardCase(
                DataTableTagGraphQlController.class, "updateDataTableTags",
                Map.of("input", new UpdateDataTableTagsInput(TABLE_ID, List.of())), tableCheck("DATA_TABLE_EDIT")),
            new GuardCase(
                DataTableWebhookGraphQlController.class, "dataTableWebhooks", tableArguments,
                tableCheck("DATA_TABLE_VIEW")));
    }

    private static Function<PermissionService, Boolean> tableCheck(String scope) {
        return permissionService -> permissionService.hasResourceScope(TABLE_ID, "DataTable", scope);
    }

    private static Function<PermissionService, Boolean> workspaceViewCheck() {
        return permissionService -> permissionService.hasWorkspaceScope(WORKSPACE_ID, "DATA_TABLE_VIEW", PRODUCTION);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, GuardCase guardCase) {
        Method method = findMethod(guardCase.controllerClass(), guardCase.methodName());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, toArguments(method, guardCase.argumentsByName()));

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(Class<?> controllerClass, String methodName) {
        List<Method> methods = Arrays.stream(controllerClass.getDeclaredMethods())
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
        Class<?> controllerClass, String methodName, Map<String, Object> argumentsByName,
        Function<PermissionService, Boolean> expectedCheck) {

        @Override
        public String toString() {
            return controllerClass.getSimpleName() + "#" + methodName;
        }
    }
}
