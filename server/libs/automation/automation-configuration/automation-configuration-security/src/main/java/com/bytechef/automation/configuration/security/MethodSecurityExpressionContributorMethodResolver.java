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

package com.bytechef.automation.configuration.security;

import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.util.List;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;

/**
 * @author Ivica Cardic
 */
final class MethodSecurityExpressionContributorMethodResolver implements MethodResolver {

    private final MethodSecurityExpressionContributor contributor;
    private final ReflectiveMethodResolver reflectiveMethodResolver = new ReflectiveMethodResolver();

    MethodSecurityExpressionContributorMethodResolver(MethodSecurityExpressionContributor contributor) {
        this.contributor = contributor;
    }

    @Override
    public MethodExecutor resolve(
        EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes)
        throws AccessException {

        if (!(targetObject instanceof AutomationMethodSecurityExpressionRoot)) {
            return null;
        }

        MethodExecutor methodExecutor = reflectiveMethodResolver.resolve(context, contributor, name, argumentTypes);

        if (methodExecutor == null) {
            return null;
        }

        return (evaluationContext, target, arguments) -> methodExecutor.execute(
            evaluationContext, contributor, arguments);
    }
}
