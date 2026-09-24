/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class TenantAdminGateTestSupport {

    private TenantAdminGateTestSupport() {
    }

    static final class GateRecorder {

        private boolean permit;
        private int callCount;

        void reset() {
            permit = false;
            callCount = 0;
        }

        void permit(boolean value) {
            permit = value;
        }

        int getCallCount() {
            return callCount;
        }

        private boolean record() {
            callCount++;

            return permit;
        }
    }

    static final class TenantAdminExpressionHandler extends DefaultMethodSecurityExpressionHandler {

        private final GateRecorder gateRecorder;

        TenantAdminExpressionHandler(GateRecorder gateRecorder) {
            this.gateRecorder = gateRecorder;
        }

        @Override
        public EvaluationContext createEvaluationContext(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

            StandardEvaluationContext evaluationContext =
                (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

            TenantAdminExpressionRoot root = new TenantAdminExpressionRoot(
                authentication, methodInvocation, gateRecorder);

            root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
            root.setPermissionEvaluator(getPermissionEvaluator());
            root.setDefaultRolePrefix(getDefaultRolePrefix());

            evaluationContext.setRootObject(root);

            return evaluationContext;
        }
    }

    private static final class TenantAdminExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

        private final GateRecorder gateRecorder;
        private final MethodInvocation methodInvocation;

        private Object filterObject;
        private Object returnObject;

        private TenantAdminExpressionRoot(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
            GateRecorder gateRecorder) {

            super(authentication::get);

            this.gateRecorder = gateRecorder;
            this.methodInvocation = methodInvocation;
        }

        public boolean isTenantAdmin() {
            return gateRecorder.record();
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
