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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.test.facade.TestWorkflowExecutor;
import com.bytechef.platform.workflow.test.web.rest.WorkflowScopeGateTestSupport.GateExpressionHandler;
import com.bytechef.platform.workflow.test.web.rest.WorkflowScopeGateTestSupport.GateRecorder;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Proves, empirically rather than by assumption, that {@code attachWorkflowTest} and {@code startWorkflowTest} are
 * actually reachable (and therefore actually gated) under production's real method-security configuration.
 *
 * <p>
 * Production's {@code SecurityConfiguration} declares {@code @EnableMethodSecurity(securedEnabled = true,
 * proxyTargetClass = true)} — {@code proxyTargetClass = true} is added by this same change, and this test is the
 * evidence for why it is safe: {@code attachWorkflowTest} and {@code startWorkflowTest} are controller-only methods,
 * not declared on the generated {@code WorkflowTestApi} interface ({@code stopWorkflowTest} is the only one of the
 * three that is). A JDK dynamic proxy for a bean implementing {@code WorkflowTestApi} would expose only that
 * interface's methods — the other two would be invisible to a caller typed as (or autowired by) the concrete controller
 * class, and therefore silently ungated for any caller that reached them some other way (e.g. a future facade
 * delegating to the bean directly, or a test wired the way this one is).
 *
 * <p>
 * This class's local {@code @EnableMethodSecurity(securedEnabled = true)} config does NOT reference
 * {@code SecurityConfiguration} at all, and so does not, by itself, guard {@code SecurityConfiguration}'s own
 * {@code proxyTargetClass = true} attribute --
 * {@link com.bytechef.security.config.SecurityConfigurationMethodSecurityTest} in {@code security-config} is the test
 * that pins that attribute directly, by reflection. What THIS class proves is the other half: that the ambient
 * {@code AopAutoConfiguration} default alone -- the real Spring Boot autoconfiguration class that forces class-based
 * (CGLIB) proxying application-wide ({@code spring.aop.proxy-target-class} defaults to {@code true}, and nothing in
 * this repository's configuration sets it {@code false} — confirmed by grepping the whole tree) -- is already enough to
 * make {@code attachWorkflowTest} and {@code startWorkflowTest} reachable, independent of
 * {@code SecurityConfiguration}'s explicit setting. If {@code AopAutoConfiguration} is ever excluded, or
 * {@code spring.aop.proxy-target-class} is ever set to {@code false} somewhere, THIS test starts failing with a
 * {@code NoSuchBeanDefinitionException} (the resulting JDK proxy would not be assignable to the concrete
 * {@code WorkflowTestApiController} type this test autowires by) -- which is exactly the scenario
 * {@code SecurityConfiguration}'s explicit {@code proxyTargetClass = true} exists to compensate for.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowTestApiControllerProductionProxyModeTest.Config.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTestApiControllerProductionProxyModeTest {

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
        when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any(), any())).thenReturn(true);

        gateRecorder.reset();
        gateRecorder.permit(true);

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
    void testAttachWorkflowTestIsReachableUnderProductionProxyDefaults() {
        assertThatCode(() -> controller.attachWorkflowTest(1L)).doesNotThrowAnyException();
    }

    @Test
    void testStartWorkflowTestIsReachableUnderProductionProxyDefaults() {
        assertThatCode(() -> controller.startWorkflowTest("workflow-id", 1L, null)).doesNotThrowAnyException();
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(securedEnabled = true)
    @Import(AopAutoConfiguration.class)
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
