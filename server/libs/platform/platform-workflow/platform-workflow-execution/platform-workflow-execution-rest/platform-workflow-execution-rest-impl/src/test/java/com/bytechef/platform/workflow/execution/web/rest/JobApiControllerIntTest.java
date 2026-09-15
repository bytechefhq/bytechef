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

package com.bytechef.platform.workflow.execution.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = JobApiController.class)
@MockitoBean(types = {
    JobFacade.class, JobService.class
})
@WebMvcTest(controllers = JobApiController.class, properties = "bytechef.coordinator.enabled=true")
public class JobApiControllerIntTest {

    @Disabled
    @Test
    public void testGetJob() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetJobTaskExecutions() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetJobs() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetLatestJob() {
        // TODO
    }

    @Disabled
    @Test
    public void testPostJob() {
        // TODO
    }

    @Disabled
    @Test
    public void testRestartJob() {
        // TODO
    }

    @Disabled
    @Test
    public void testStopJob() {
        // TODO
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long JOB_ID = 7L;

        @Autowired
        private JobApi jobApi;

        @Autowired
        private JobFacade jobFacade;

        @Autowired
        private JobService jobService;

        @Autowired
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

            doThrow(bodyReachedException).when(jobFacade)
                .resumeJob(anyLong());
            doThrow(bodyReachedException).when(jobFacade)
                .stopJob(anyLong());
            doThrow(bodyReachedException).when(jobService)
                .fetchLastJob();
            doThrow(bodyReachedException).when(jobService)
                .getJob(anyLong());
            doThrow(bodyReachedException).when(jobService)
                .getJobsPage(anyInt());

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest
        @MethodSource("guardCases")
        void testGuardDeniesWhenItsPermissionCheckIsRefused(GuardCase guardCase) {
            assertThatThrownBy(() -> guardCase.guardedOperation()
                .invoke(jobApi))
                    .isInstanceOf(AccessDeniedException.class);

            guardCase.expectedCheck()
                .apply(verify(permissionService));
        }

        @ParameterizedTest
        @MethodSource("guardCases")
        void testGuardAllowsWhenItsPermissionCheckIsGranted(GuardCase guardCase) {
            when(guardCase.expectedCheck()
                .apply(permissionService)).thenReturn(true);

            assertThatThrownBy(() -> guardCase.guardedOperation()
                .invoke(jobApi))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames = Arrays.stream(JobApiController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

            Set<String> coveredMethodNames = guardCases()
                .map(Named::getName)
                .map(name -> name.split(" ")[0])
                .collect(Collectors.toSet());

            assertThat(coveredMethodNames).isEqualTo(guardedMethodNames);
        }

        static Stream<Named<GuardCase>> guardCases() {
            return Stream.of(
                Named.of(
                    "getJob",
                    new GuardCase(
                        guardedApi -> guardedApi.getJob(JOB_ID),
                        permissionService -> permissionService.hasResourceScope(JOB_ID, "Job", "EXECUTION_VIEW"))),
                Named.of(
                    "getJobsPage",
                    new GuardCase(guardedApi -> guardedApi.getJobsPage(0), PermissionService::isTenantAdmin)),
                Named.of(
                    "getLatestJob",
                    new GuardCase(JobApi::getLatestJob, PermissionService::isTenantAdmin)),
                Named.of(
                    "restartJob",
                    new GuardCase(
                        guardedApi -> guardedApi.restartJob(JOB_ID),
                        permissionService -> permissionService.hasResourceScope(JOB_ID, "Job", "DEPLOYMENT_EDIT"))),
                Named.of(
                    "stopJob",
                    new GuardCase(
                        guardedApi -> guardedApi.stopJob(JOB_ID),
                        permissionService -> permissionService.hasResourceScope(JOB_ID, "Job", "DEPLOYMENT_EDIT"))));
        }

        @FunctionalInterface
        interface GuardedOperation {

            void invoke(JobApi guardedApi);
        }

        record GuardCase(GuardedOperation guardedOperation, Function<PermissionService, Boolean> expectedCheck) {
        }

        @EnableMethodSecurity
        @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
        static class MethodSecurityConfiguration {

            @Bean
            PermissionService permissionService() {
                return mock(PermissionService.class, MockReset.withSettings(MockReset.AFTER));
            }
        }
    }
}
