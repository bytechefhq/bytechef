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

import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Stands in for {@code AutomationMethodSecurityExpressionHandler} so this module's {@code @PreAuthorize} expressions
 * can be evaluated in a test context.
 * <p>
 * The real handler lives in {@code automation-configuration-service}; this module is a platform module and must not
 * depend on it, so the SpEL function is re-declared here rather than imported. That means these tests pin the
 * <em>wiring</em> — that the expression parses, resolves to a method of this name and arity, and receives the caller's
 * own arguments — while the function's own semantics (ordinal resolution, fail-closed on an unidentifiable environment,
 * skip-mode ordering) are pinned by {@code AutomationMethodSecurityExpressionRootTest} beside the production
 * implementation.
 *
 * @author Ivica Cardic
 */
final class WorkflowScopeGateTestSupport {

    private WorkflowScopeGateTestSupport() {
    }

    /**
     * Records the arguments the SpEL expression actually passed, so a test can assert the gate reads the caller's own
     * environment rather than a constant or the wrong property.
     */
    static final class GateRecorder {

        private boolean permit;
        private String workflowId;
        private String scope;
        private Long environmentId;
        private int callCount;

        void reset() {
            permit = false;
            workflowId = null;
            scope = null;
            environmentId = null;
            callCount = 0;
        }

        void permit(boolean value) {
            permit = value;
        }

        String getWorkflowId() {
            return workflowId;
        }

        String getScope() {
            return scope;
        }

        Long getEnvironmentId() {
            return environmentId;
        }

        int getCallCount() {
            return callCount;
        }

        private boolean record(String recordedWorkflowId, String recordedScope, Long recordedEnvironmentId) {
            workflowId = recordedWorkflowId;
            scope = recordedScope;
            environmentId = recordedEnvironmentId;
            callCount++;

            return permit;
        }
    }

    static final class GateExpressionHandler extends DefaultMethodSecurityExpressionHandler {

        private final GateRecorder gateRecorder;

        GateExpressionHandler(GateRecorder gateRecorder) {
            this.gateRecorder = gateRecorder;
        }

        @Override
        public EvaluationContext createEvaluationContext(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

            StandardEvaluationContext context =
                (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

            GateExpressionRoot root = new GateExpressionRoot(authentication, methodInvocation, gateRecorder);

            root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
            root.setPermissionEvaluator(getPermissionEvaluator());
            root.setDefaultRolePrefix(getDefaultRolePrefix());

            context.setRootObject(root);

            return context;
        }
    }

    static final class GateExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

        private final MethodInvocation methodInvocation;
        private final GateRecorder gateRecorder;

        private Object filterObject;
        private Object returnObject;

        GateExpressionRoot(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
            GateRecorder gateRecorder) {

            super(authentication::get);

            this.methodInvocation = methodInvocation;
            this.gateRecorder = gateRecorder;
        }

        public boolean hasWorkflowScopeInEnvironment(String workflowId, String scope, Long environmentId) {
            return gateRecorder.record(workflowId, scope, environmentId);
        }

        @Override
        public Object getFilterObject() {
            return filterObject;
        }

        @Override
        public void setFilterObject(Object filterObject) {
            this.filterObject = filterObject;
        }

        @Override
        public Object getReturnObject() {
            return returnObject;
        }

        @Override
        public void setReturnObject(Object returnObject) {
            this.returnObject = returnObject;
        }

        @Override
        public Object getThis() {
            return methodInvocation.getThis();
        }
    }
}
