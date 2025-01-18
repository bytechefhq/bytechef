/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.ComponentHandler;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
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
public class ComponentHandlerLoader {

    public static ComponentHandler loadComponentHandler(URL url, Language language, String cacheKey) {
        try {
            return switch (language) {
                case JAVA -> loadJavaComponentHandler(url, cacheKey);
                case JAVASCRIPT, PYTHON, RUBY -> loadPolyglotComponentHandler(url, language);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ComponentHandler loadJavaComponentHandler(URL url, String cacheKey) throws IOException {
        try (ComponentHandlerClassLoader codeComponentHandlerClassLoader = ComponentHandlerClassLoader.of(
            url, cacheKey)) {

            return codeComponentHandlerClassLoader.loadComponentHandler();
        }
    }

    private static ComponentHandler loadPolyglotComponentHandler(URL url, Language language)
        throws URISyntaxException, IOException {

        return ComponentHandlerPolyglotEngine.load(getLanguageId(language), Files.readString(Paths.get(url.toURI())));
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
