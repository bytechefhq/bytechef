/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.workflow.ProjectHandler;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectHandlerLoader {

    public static ProjectHandler loadProjectHandler(URL url, Language language, String cacheKey) {
        try {
            return switch (language) {
                case JAVA -> loadJavaProjectHandler(url, cacheKey);
                case JAVASCRIPT, PYTHON, RUBY -> loadPolyglotProjectHandler(url, language);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ProjectHandler loadJavaProjectHandler(URL url, String cacheKey) throws IOException {
        try (ProjectHandlerClassLoader projectHandlerClassLoader = ProjectHandlerClassLoader.of(
            url, cacheKey)) {

            return projectHandlerClassLoader.loadWorkflowHandler();
        }
    }

    private static ProjectHandler loadPolyglotProjectHandler(URL url, Language language)
        throws URISyntaxException, IOException {

        return ProjectHandlerPolyglotEngine.load(getLanguageId(language), Files.readString(Paths.get(url.toURI())));
    }

    private static String getLanguageId(Language language) {
        return switch (language) {
//            case JAVA -> "java";
            case JAVASCRIPT -> "js";
            case PYTHON -> "python";
            case RUBY -> "ruby";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
}
