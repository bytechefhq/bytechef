/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
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
 * @version ee
 *
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
