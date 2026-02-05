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

package com.bytechef.platform.configuration.cache;

import com.bytechef.platform.configuration.annotation.WorkflowCacheEvict;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link WorkflowCacheEvict} and clears the specified caches.
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
public class WorkflowCacheEvictAspect {

    private final WorkflowCacheManager workflowCacheManager;

    @SuppressFBWarnings("EI")
    public WorkflowCacheEvictAspect(WorkflowCacheManager workflowCacheManager) {
        this.workflowCacheManager = workflowCacheManager;
    }

    /**
     * Intercepts methods annotated with {@link WorkflowCacheEvict} and clears the specified caches.
     *
     * @param joinPoint          the join point
     * @param result             the result of the method execution
     * @param workflowCacheEvict the annotation
     */
    @AfterReturning(pointcut = "@annotation(workflowCacheEvict)", returning = "result")
    public void clearCache(JoinPoint joinPoint, Object result, WorkflowCacheEvict workflowCacheEvict) {
        Arguments arguments = extractArguments(joinPoint);

        for (String cacheName : workflowCacheEvict.cacheNames()) {
            workflowCacheManager.clearCacheForWorkflow(arguments.workflowId, cacheName, arguments.environmentId);
        }
    }

    /**
     * Extracts the workflow ID and environment ID from the method parameters based on parameter annotations.
     *
     * @param joinPoint the join point
     * @return the Arguments containing workflow ID and environment ID
     */
    private Arguments extractArguments(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        Method method = methodSignature.getMethod();

        Parameter[] parameters = method.getParameters();

        Long environmentId = null;
        String workflowId = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(WorkflowCacheEvict.EnvironmentIdParam.class)) {
                environmentId = (Long) args[i];
            } else if (parameter.isAnnotationPresent(WorkflowCacheEvict.WorkflowIdParam.class)) {
                workflowId = (String) args[i];
            }
        }

        return new Arguments(Objects.requireNonNull(environmentId, "environmentId"), workflowId);
    }

    private record Arguments(long environmentId, String workflowId) {
    }
}
