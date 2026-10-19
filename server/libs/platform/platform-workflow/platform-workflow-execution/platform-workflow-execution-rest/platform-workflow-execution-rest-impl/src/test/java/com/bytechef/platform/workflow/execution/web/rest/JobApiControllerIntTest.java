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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    JobApiController.class, JobApiControllerIntTest.JobFacadeConfiguration.class
})
@MockitoBean(types = {
    JobResumeFacade.class, JobService.class
})
@WebMvcTest(controllers = JobApiController.class, properties = "bytechef.coordinator.enabled=true")
class JobApiControllerIntTest {

    @Nested
    class RestartAndStop {

        private static final long JOB_ID = 42L;

        @MockitoBean
        private JobFacade jobFacade;

        @Autowired
        private JobResumeFacade jobResumeFacade;

        @Autowired
        private MockMvc mockMvc;

        @Test
        void testRestartJobRestartsAStoppedJob() throws Exception {
            when(jobFacade.getJob(JOB_ID)).thenReturn(stoppedJob(Map.of()));

            mockMvc.perform(put("/internal/jobs/{id}/restart", JOB_ID))
                .andExpect(status().isNoContent());

            verify(jobFacade).resumeJob(JOB_ID);
        }

        @Test
        void testRestartJobRejectsAJobWaitingOnAnApproval() throws Exception {
            when(jobFacade.getJob(JOB_ID)).thenReturn(
                stoppedJob(
                    Map.of(MetadataConstants.JOB_RESUME_ID, "resumeId", MetadataConstants.APPROVAL_RESUME, true)));

            mockMvc.perform(put("/internal/jobs/{id}/restart", JOB_ID))
                .andExpect(status().isConflict());

            verify(jobFacade, never()).resumeJob(anyLong());
        }

        @Test
        void testStopJobCancelsAJobWaitingOnAnApproval() throws Exception {
            when(jobResumeFacade.cancelApproval(JOB_ID)).thenReturn(true);

            mockMvc.perform(put("/internal/jobs/{id}/stop", JOB_ID))
                .andExpect(status().isNoContent());

            verify(jobFacade, never()).stopJob(anyLong());
        }

        @Test
        void testStopJobStopsARunningJob() throws Exception {
            when(jobResumeFacade.cancelApproval(JOB_ID)).thenReturn(false);

            mockMvc.perform(put("/internal/jobs/{id}/stop", JOB_ID))
                .andExpect(status().isNoContent());

            verify(jobFacade).stopJob(JOB_ID);
        }

        private static Job stoppedJob(Map<String, Object> metadata) {
            Job job = new Job(JOB_ID);

            job.setMetadata(metadata);
            job.setStatus(Job.Status.STOPPED);

            return job;
        }
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long JOB_ID = 7L;

        @Autowired
        private JobApi jobApi;

        @Autowired
        private JobService jobService;

        @Autowired
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

            doThrow(bodyReachedException).when(jobService)
                .resumeToStatusStarted(anyLong());
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
            guardCase.arrangement()
                .accept(permissionService, jobService);

            assertThatThrownBy(() -> guardCase.guardedOperation()
                .invoke(jobApi))
                    .isInstanceOf(AccessDeniedException.class);

            guardCase.expectedCheck()
                .apply(verify(permissionService));
        }

        @ParameterizedTest
        @MethodSource("guardCases")
        void testGuardAllowsWhenItsPermissionCheckIsGranted(GuardCase guardCase) {
            guardCase.arrangement()
                .accept(permissionService, jobService);

            when(guardCase.expectedCheck()
                .apply(permissionService)).thenReturn(true);

            assertThatThrownBy(() -> guardCase.guardedOperation()
                .invoke(jobApi))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames = Arrays.stream(JobFacadeImpl.class.getDeclaredMethods())
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
                        permissionService -> permissionService.hasResourceScope(JOB_ID, "Job",
                            "EXECUTION_DETAILS_VIEW"))),
                Named.of(
                    "getJobsPage",
                    new GuardCase(guardedApi -> guardedApi.getJobsPage(0), PermissionService::isTenantAdmin)),
                Named.of(
                    "getLatestJob",
                    new GuardCase(JobApi::getLatestJob, PermissionService::isTenantAdmin)),
                Named.of(
                    "resumeJob through restartJob",
                    new GuardCase(
                        guardedApi -> guardedApi.restartJob(JOB_ID),
                        permissionService -> permissionService.hasResourceScope(JOB_ID, "Job", "DEPLOYMENT_EDIT"),
                        (permissionService, jobService) -> {
                            when(permissionService.hasResourceScope(JOB_ID, "Job", "EXECUTION_DETAILS_VIEW"))
                                .thenReturn(true);

                            doReturn(new Job(JOB_ID)).when(jobService)
                                .getJob(JOB_ID);
                        })),
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

        record GuardCase(
            GuardedOperation guardedOperation, Function<PermissionService, Boolean> expectedCheck,
            BiConsumer<PermissionService, JobService> arrangement) {

            GuardCase(GuardedOperation guardedOperation, Function<PermissionService, Boolean> expectedCheck) {
                this(guardedOperation, expectedCheck, (permissionService, jobService) -> {});
            }
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

    static class JobFacadeConfiguration {

        @Bean
        JobFacade jobFacade(JobService jobService) {
            ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

            doThrow(new IllegalStateException("body reached")).when(applicationEventPublisher)
                .publishEvent(any(Object.class));

            return new JobFacadeImpl(
                applicationEventPublisher, mock(ContextService.class), jobService, mock(TaskExecutionService.class),
                mock(TaskFileStorage.class), mock(WorkflowService.class));
        }
    }
}
