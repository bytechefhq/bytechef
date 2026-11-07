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

package com.bytechef.platform.workflow.task.dispatcher.test.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * @author Ivica Cardic
 */
final class DeferredEvaluationParameterKeysLoader {

    private static final String BASE_PACKAGE = "com.bytechef.task.dispatcher";

    private DeferredEvaluationParameterKeysLoader() {
    }

    static void loadAll() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));

        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            String beanClassName = beanDefinition.getBeanClassName();

            if (beanClassName == null) {
                continue;
            }

            Thread currentThread = Thread.currentThread();

            try {
                Class.forName(beanClassName, true, currentThread.getContextClassLoader());
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException(
                    "Unable to load task dispatcher configuration class: " + beanClassName, classNotFoundException);
            }
        }
    }
}
