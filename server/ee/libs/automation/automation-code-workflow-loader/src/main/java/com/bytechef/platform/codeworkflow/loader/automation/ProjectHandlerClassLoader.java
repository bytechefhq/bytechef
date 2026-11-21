/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.classloader.AbstractHandlerClassLoader;
import com.bytechef.classloader.IsolatingClassLoader;
import com.bytechef.classloader.util.ClassLoaderUtils;
import java.net.URL;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerClassLoader extends AbstractHandlerClassLoader<ProjectHandler> {

    private ProjectHandlerClassLoader(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        super(jarUrl, cacheKey, cacheManager, ProjectHandler.class, ContextClassLoaderProjectHandler::new);
    }

    static ProjectHandlerClassLoader of(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        return new ProjectHandlerClassLoader(jarUrl, cacheKey, cacheManager);
    }

    private record ContextClassLoaderProjectHandler(
        ProjectHandler projectHandler, IsolatingClassLoader<?> classLoader) implements ProjectHandler {

        @Override
        public ProjectDefinition getDefinition() {
            return ClassLoaderUtils.loadWithClassLoader(classLoader, projectHandler::getDefinition);
        }
    }
}
