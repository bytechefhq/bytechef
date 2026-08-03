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
 * Forces the classloading -- and therefore the static {@code DeferredEvaluationParameterKeys.register(...)} calls -- of
 * every {@code *TaskDispatcherConfiguration} class reachable on the test classpath, without registering any of them as
 * Spring beans.
 *
 * <p>
 * Every {@code *TaskDispatcherIntTest} (see {@code TaskDispatcherJobTestExecutor}) constructs its
 * {@code TaskDispatcher}/{@code TaskCompletionHandler} instances directly, bypassing the module's real
 * {@code *TaskDispatcherConfiguration} Spring bean entirely -- so, left alone, that class's static initializer (where
 * dispatchers like {@code condition}, {@code branch}, {@code graph}, and {@code on-error} call
 * {@code DeferredEvaluationParameterKeys.register(...)} to keep their sub-task expressions from being evaluated before
 * the branch they belong to is actually chosen) never runs in this harness. Deferred parameter keys are corrupted
 * silently -- eagerly evaluated instead of preserved -- rather than failing loudly, so the gap is easy to miss until a
 * fixture happens to depend on it.
 * </p>
 *
 * <p>
 * Registering these {@code @Configuration} classes as real Spring beans in {@code TaskDispatcherIntTestConfiguration}
 * is not an option: their {@code @Autowired} fields (e.g. {@code ContextService}, {@code Evaluator}) point at beans
 * this harness never defines, since {@code TaskDispatcherJobTestExecutor#execute} builds a throwaway instance of each
 * per test execution instead. This class sidesteps that by loading just the CLASS -- via {@code Class.forName}, which
 * runs static initializers without ever asking Spring to instantiate it or satisfy its dependencies.
 * </p>
 *
 * <p>
 * Candidate classes are discovered by scanning {@code com.bytechef.task.dispatcher} for {@code @Configuration} types,
 * rather than by hand-listing known modules: a scan finds every {@code *TaskDispatcherConfiguration} actually present
 * on a given test's classpath (which varies -- e.g. {@code graph}'s tests also pull in {@code condition} and
 * {@code fork-join} for its combo fixtures), and automatically covers any future dispatcher module that registers
 * deferred keys the same way, with no harness change required.
 * </p>
 *
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

            try {
                Class.forName(beanClassName, true, Thread.currentThread()
                    .getContextClassLoader());
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException(
                    "Unable to load task dispatcher configuration class: " + beanClassName, classNotFoundException);
            }
        }
    }
}
