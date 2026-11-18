/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.component.polyglot.AbstractHandlerLoader;
import java.net.URL;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectHandlerLoader extends AbstractHandlerLoader<ProjectHandler> {

    private static final ProjectHandlerLoader INSTANCE = new ProjectHandlerLoader();

    private ProjectHandlerLoader() {
    }

    public static ProjectHandler
        loadProjectHandler(URL url, Language language, String cacheKey, CacheManager cacheManager) {
        return INSTANCE.loadHandler(url, language, cacheKey, cacheManager);
    }

    @Override
    protected ProjectHandler loadJavaHandler(URL url, String cacheKey, CacheManager cacheManager) {
        ProjectHandlerClassLoader projectHandlerClassLoader = ProjectHandlerClassLoader.of(url, cacheKey, cacheManager);

        return projectHandlerClassLoader.loadHandler();
    }

    @Override
    protected ProjectHandler loadPolyglotHandler(String languageId, String script) {
        return ProjectHandlerPolyglotEngine.load(languageId, script);
    }
}
