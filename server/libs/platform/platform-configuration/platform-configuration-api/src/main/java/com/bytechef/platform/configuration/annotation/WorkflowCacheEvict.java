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

package com.bytechef.platform.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should trigger cache cleaning for a workflow. The aspect will intercept these methods
 * and call the CacheCleanManager to clear the specified caches.
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WorkflowCacheEvict {

    /**
     * The names of the caches to clear.
     *
     * @return array of cache names
     */
    String[] cacheNames();

    /**
     * The name of the parameter that contains the workflow ID. If not specified, the aspect will try to find a
     * parameter named "workflowId" or a method "getId()" on a parameter named "workflow".
     *
     * @return the name of the parameter that contains the workflow ID
     */
    String workflowIdParam() default "";
}
