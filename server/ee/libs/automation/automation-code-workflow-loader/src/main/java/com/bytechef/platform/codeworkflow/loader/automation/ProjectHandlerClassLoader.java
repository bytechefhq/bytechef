/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.classloader.IsolatingClassLoader;
import com.bytechef.classloader.util.ClassLoaderUtils;
import java.net.URL;
import java.util.ServiceLoader;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerClassLoader extends IsolatingClassLoader<ProjectHandler> {

    private final String cacheKey;

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private ProjectHandlerClassLoader(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        super(
            new URL[] {
                jarUrl
            }, ProjectHandlerClassLoader.class.getClassLoader(),
            cacheManager,
            (key, classLoader) -> {
                ServiceLoader<ProjectHandler> loader = ServiceLoader.load(ProjectHandler.class, classLoader);

                for (ProjectHandler projectHandler : loader) {
                    return new ContextClassLoaderProjectHandler(projectHandler, classLoader);
                }

                return null;
            });

        this.cacheKey = cacheKey;
    }

    static ProjectHandlerClassLoader of(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        return new ProjectHandlerClassLoader(jarUrl, cacheKey, cacheManager);
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
