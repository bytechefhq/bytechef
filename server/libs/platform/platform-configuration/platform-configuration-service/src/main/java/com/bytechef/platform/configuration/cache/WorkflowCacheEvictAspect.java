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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.annotation.WorkflowCacheEvict;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
        String workflowId = extractWorkflowId(joinPoint, workflowCacheEvict);

        if (workflowId != null) {
            for (String cacheName : workflowCacheEvict.cacheNames()) {
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName);
            }
        }
    }

    /**
     * Extracts the workflow ID from the method parameters based on the annotation configuration.
     *
     * @param joinPoint          the join point
     * @param workflowCacheEvict the annotation
     * @return the workflow ID or null if not found
     */
    private String extractWorkflowId(JoinPoint joinPoint, WorkflowCacheEvict workflowCacheEvict) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String workflowId = workflowCacheEvict.workflowIdParam();

        Method method = methodSignature.getMethod();

        Parameter[] parameters = method.getParameters();

        if (!workflowId.isEmpty()) {
            for (int i = 0; i < parameters.length; i++) {
                String name = parameters[i].getName();

                if (name.equals(workflowId) && args[i] instanceof String) {
                    return (String) args[i];
                }
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            String name = parameters[i].getName();

            if (name.equals("workflowId") && args[i] instanceof String) {
                return (String) args[i];
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            String name = parameters[i].getName();

            if (name.equals("workflow") && args[i] instanceof Workflow workflow) {
                return workflow.getId();
            }
        }

        return null;
    }
}
