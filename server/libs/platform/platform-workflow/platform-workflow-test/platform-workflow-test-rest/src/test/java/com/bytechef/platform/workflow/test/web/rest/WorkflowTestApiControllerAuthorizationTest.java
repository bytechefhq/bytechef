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

package com.bytechef.platform.workflow.test.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.test.facade.TestWorkflowExecutor;
import com.bytechef.platform.workflow.test.web.rest.WorkflowScopeGateTestSupport.GateExpressionHandler;
import com.bytechef.platform.workflow.test.web.rest.WorkflowScopeGateTestSupport.GateRecorder;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Behavioral and reflection coverage for the {@code @PreAuthorize} gates closing the ticket-1051 hole on
 * {@link WorkflowTestApiController}: {@code startWorkflowTest}, {@code attachWorkflowTest} and {@code stopWorkflowTest}
 * previously carried no authorization at all, so any authenticated principal in the tenant could execute any workflow
 * (using its stored connections), stream any job's output, or stop any running job.
 *
 * <p>
 * {@code startWorkflowTest} keys on the workflow id and requires {@code WORKFLOW_EDIT} — the same scope
 * {@code WorkflowNodeScriptFacadeImpl#testWorkflowNodeScript}/{@code #testClusterElementScript} use, since both run a
 * workflow's code with its stored credentials. {@code attachWorkflowTest} and {@code stopWorkflowTest} key on the job
 * id and require {@code EXECUTION_VIEW}, the scope {@code LogFileStorageImpl} already uses for job-keyed reads; no
 * stronger {@code Job}-scoped token exists ({@code EXECUTION_DELETE} is the only other one, and it denotes deleting
 * job/log records, not stopping a run), so {@code EXECUTION_VIEW} is used for the stop mutation too rather than
 * inventing a new scope.
 *
 * <p>
 * {@code stopWorkflowTest} takes a {@code String jobId} while {@code attachWorkflowTest} takes {@code Long jobId} —
 * kept as-is rather than aligned, because {@code stopWorkflowTest} is the one of the three declared on the generated
 * {@code WorkflowTestApi} interface, and widening its {@code String} path variable to {@code Long} would mean editing
 * {@code openapi.yaml} and regenerating both the Spring interface and the TypeScript client, rippling into every client
 * call site that currently threads a {@code String} job id through local state and {@code localStorage}. {@code Job}'s
 * {@code ResourceOwnershipResolver} only recognizes numeric ({@code Number}) ids in its default
 * {@code resolveOwner(Serializable)} — a bare {@code String} target id resolves to {@code ResourceOwner.unknown()} and
 * therefore denies every caller, including a legitimate one. The {@code stopWorkflowTest} expression therefore converts
 * the path variable to a {@code long} inside the SpEL itself via
 * {@code T(org.apache.commons.lang3.math.NumberUtils).toLong(#jobId, -1L)} before calling {@code hasPermission}, which
 * {@link #testStopWorkflowTestPermitsCallerWithScope()} proves by asserting the evaluator actually observes a
 * {@code Long}, not the raw {@code String} path variable. {@code NumberUtils.toLong} was chosen over the simpler
 * {@code T(java.lang.Long).valueOf(#jobId)} specifically because the latter throws {@code NumberFormatException} for a
 * non-numeric {@code jobId} <em>during security evaluation</em>, before the method body's own
 * {@code jobId.matches("\\d+")} guard ever runs, surfacing as an uncaught 5xx instead of a clean deny;
 * {@code NumberUtils.toLong(#jobId, -1L)} instead falls back to {@code -1} (a job id that can never exist) so a
 * malformed {@code jobId} is denied with an ordinary {@code AccessDeniedException} —
 * {@link #testStopWorkflowTestDeniesMalformedJobIdCleanlyRatherThanThrowing()} pins this.
 *
 * <p>
 * Method security is enabled with {@code proxyTargetClass = true} because {@code attachWorkflowTest} and
 * {@code startWorkflowTest} are controller-only endpoints not declared on the generated {@code WorkflowTestApi}
 * interface ({@code stopWorkflowTest} is the only one of the three that is) — a JDK dynamic proxy (the
 * {@code @EnableMethodSecurity} default) would only expose the interface's methods, not these two.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowTestApiControllerAuthorizationTest.Config.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTestApiControllerAuthorizationTest {

    private static final String WORKFLOW_ID = "workflow-under-test";
    private static final Long ATTACH_JOB_ID = 42L;
    private static final String STOP_JOB_ID = "42";

    @Autowired
    private WorkflowTestApiController controller;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private GateRecorder gateRecorder;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        reset(permissionEvaluator);

        gateRecorder.reset();

        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(anyString(), any(), any())).thenReturn(List.of());
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testStartWorkflowTestDeniesCallerWithoutWorkflowEditScope() {
        gateRecorder.permit(false);

        assertThatThrownBy(() -> controller.startWorkflowTest(WORKFLOW_ID, 1L, null))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testStartWorkflowTestPermitsCallerWithWorkflowEditScope() {
        gateRecorder.permit(true);

        assertThatCode(() -> controller.startWorkflowTest(WORKFLOW_ID, 1L, null)).doesNotThrowAnyException();
    }

    @Test
    void testAttachWorkflowTestDeniesCallerWithoutExecutionViewScope() {
        when(permissionEvaluator.hasPermission(
            any(Authentication.class), eq(ATTACH_JOB_ID), eq("Job"), eq("EXECUTION_VIEW")))
                .thenReturn(false);

        assertThatThrownBy(() -> controller.attachWorkflowTest(ATTACH_JOB_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAttachWorkflowTestPermitsCallerWithExecutionViewScope() {
        when(permissionEvaluator.hasPermission(
            any(Authentication.class), eq(ATTACH_JOB_ID), eq("Job"), eq("EXECUTION_VIEW")))
                .thenReturn(true);

        assertThatCode(() -> controller.attachWorkflowTest(ATTACH_JOB_ID)).doesNotThrowAnyException();
    }

    @Test
    void testStopWorkflowTestDeniesCallerWithoutExecutionViewScope() {
        when(permissionEvaluator.hasPermission(
            any(Authentication.class), eq(42L), eq("Job"), eq("EXECUTION_VIEW")))
                .thenReturn(false);

        assertThatThrownBy(() -> controller.stopWorkflowTest(STOP_JOB_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Also proves the {@code String}-to-{@code Long} conversion in the {@code stopWorkflowTest} SpEL: the stub only
     * matches a {@code Long} argument, so a regression that reverted the expression to
     * {@code hasPermission(#jobId, ...)} (passing the raw {@code String}) would leave this stub unmatched, the mock
     * would fall back to its default {@code false}, and the call would be denied instead of permitted — failing this
     * test rather than silently reintroducing the always-deny bug.
     */
    @Test
    void testStopWorkflowTestPermitsCallerWithScope() {
        when(permissionEvaluator.hasPermission(
            any(Authentication.class), eq(42L), eq("Job"), eq("EXECUTION_VIEW")))
                .thenReturn(true);

        assertThatCode(() -> controller.stopWorkflowTest(STOP_JOB_ID)).doesNotThrowAnyException();
    }

    /**
     * A non-numeric {@code jobId} must never reach {@code testWorkflowExecutor.stop(Long.parseLong(jobId))} — it should
     * be denied at the security layer instead. Also proves the {@code NumberUtils.toLong(#jobId, -1L)} choice over the
     * simpler {@code T(java.lang.Long).valueOf(#jobId)}: the latter would throw {@code NumberFormatException} out of
     * {@code @PreAuthorize} evaluation for this input, which AssertJ would report as neither an
     * {@code AccessDeniedException} nor "no exception" — failing this test with a type mismatch rather than the clean
     * deny asserted here.
     */
    @Test
    void testStopWorkflowTestDeniesMalformedJobIdCleanlyRatherThanThrowing() {
        assertThatThrownBy(() -> controller.stopWorkflowTest("not-a-number"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * A numeric-but-overflowing {@code jobId} ({@code Long.parseLong} throws {@code NumberFormatException} on it)
     * collapses to the same {@code -1} fallback as a non-numeric one once it reaches {@code NumberUtils.toLong(#jobId,
     * -1L)} in the {@code @PreAuthorize} expression -- so a caller for whom the gate permits any id regardless of what
     * it resolves to (a tenant admin, whose {@code hasResourceScope} short-circuits on {@code isTenantAdmin()} before
     * resolving an owner; simulated here by stubbing the evaluator to permit id {@code -1}) reaches the method body
     * with a {@code jobId} that still cannot be parsed as a {@code Long}. Before this fix the body only guarded against
     * non-digit input ({@code jobId.matches("\\d+")}), so an all-digits-but-overflowing id passed that guard and blew
     * up {@code Long.parseLong(jobId)} with an uncaught {@code NumberFormatException} -- a 500 instead of a clean 400.
     */
    @Test
    void testStopWorkflowTestReturnsBadRequestForOverflowingJobIdWhenGatePermits() {
        String overflowingJobId = "99999999999999999999";

        when(permissionEvaluator.hasPermission(any(Authentication.class), eq(-1L), eq("Job"), eq("EXECUTION_VIEW")))
            .thenReturn(true);

        ResponseEntity<Void> response = controller.stopWorkflowTest(overflowingJobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testStartWorkflowTestExpressionRequiresWorkflowEdit() {
        assertExpression("startWorkflowTest", "hasWorkflowScopeInEnvironment(#id, 'WORKFLOW_EDIT', #environmentId)");
    }

    /**
     * The point of the gate: the environment it authorises must be the one the run will actually use. Gating the
     * workflow alone unions the environments the caller can reach, so a member who is editor in Development could start
     * a Production run by changing this request parameter alone.
     */
    @Test
    void testStartWorkflowTestGatesTheEnvironmentIdFromTheRequest() {
        gateRecorder.permit(true);

        controller.startWorkflowTest(WORKFLOW_ID, 2L, null);

        assertThat(gateRecorder.getCallCount()).isOne();
        assertThat(gateRecorder.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(gateRecorder.getScope()).isEqualTo("WORKFLOW_EDIT");
        assertThat(gateRecorder.getEnvironmentId()).isEqualTo(2L);
    }

    @Test
    void testAttachWorkflowTestExpressionRequiresExecutionView() {
        assertExpression("attachWorkflowTest", "hasPermission(#jobId, 'Job', 'EXECUTION_VIEW')");
    }

    @Test
    void testStopWorkflowTestExpressionRequiresExecutionView() {
        assertExpression(
            "stopWorkflowTest",
            "hasPermission(T(org.apache.commons.lang3.math.NumberUtils).toLong(#jobId, -1L), 'Job', "
                + "'EXECUTION_VIEW')");
    }

    private static void assertExpression(String methodName, String expression) {
        Method match = null;

        for (Method candidate : WorkflowTestApiController.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method %s", methodName)
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo(expression);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {

        @Bean
        WorkflowTestApiController workflowTestApiController(
            TempFileStorage tempFileStorage, TestWorkflowExecutor testWorkflowExecutor,
            WorkflowService workflowService) {

            return new WorkflowTestApiController(tempFileStorage, testWorkflowExecutor, workflowService);
        }

        @Bean
        TempFileStorage tempFileStorage() {
            return mock(TempFileStorage.class);
        }

        @Bean
        TestWorkflowExecutor testWorkflowExecutor() {
            return mock(TestWorkflowExecutor.class);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }

        @Bean
        PermissionEvaluator permissionEvaluator() {
            return mock(PermissionEvaluator.class);
        }

        @Bean
        GateRecorder gateRecorder() {
            return new GateRecorder();
        }

        @Bean
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator, GateRecorder gateRecorder) {

            GateExpressionHandler handler = new GateExpressionHandler(gateRecorder);

            handler.setPermissionEvaluator(permissionEvaluator);

            return handler;
        }
    }
}
