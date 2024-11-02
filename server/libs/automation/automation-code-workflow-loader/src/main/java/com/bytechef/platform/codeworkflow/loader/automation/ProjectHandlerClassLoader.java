/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.classloader.IsolatingClassLoader;
import com.bytechef.classloader.util.ClassLoaderUtils;
import com.bytechef.workflow.ProjectHandler;
import com.bytechef.workflow.definition.ProjectDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import java.util.ServiceLoader;

/**
 * @author Ivica Cardic
 */
class ProjectHandlerClassLoader extends IsolatingClassLoader<ProjectHandler> {

    private final String cacheKey;

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private ProjectHandlerClassLoader(URL jarUrl, String cacheKey) {
        super(
            new URL[] {
                jarUrl
            }, ProjectHandlerClassLoader.class.getClassLoader(),
            (key, classLoader) -> {
                ServiceLoader<ProjectHandler> loader = ServiceLoader.load(ProjectHandler.class, classLoader);

                for (ProjectHandler projectHandler : loader) {
                    return new ContextClassLoaderProjectHandler(projectHandler, classLoader);
                }

                return null;
            });

        this.cacheKey = cacheKey;
    }

    @SuppressFBWarnings("DP")
    static ProjectHandlerClassLoader of(URL jarUrl, String cacheKey) {
        return new ProjectHandlerClassLoader(jarUrl, cacheKey);
    }

    ProjectHandler loadWorkflowHandler() {
        return get(cacheKey);
    }

    /**
     * A WorkflowHandler that wraps another WorkflowHandler, using the given class loader as the thread's context class
     * loader for all its invocations.
     *
     * @author Ivica Cardic
     */
    private record ContextClassLoaderProjectHandler(
        ProjectHandler projectHandler, IsolatingClassLoader<?> classLoader) implements ProjectHandler {

        @Override
        public ProjectDefinition getDefinition() {
            return ClassLoaderUtils.loadWithClassLoader(classLoader, projectHandler::getDefinition);
        }
    }
}
