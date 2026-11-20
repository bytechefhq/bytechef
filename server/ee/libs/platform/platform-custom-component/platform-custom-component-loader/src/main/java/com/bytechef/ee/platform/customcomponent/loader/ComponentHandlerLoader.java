/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentHandlerLoader {

    /**
     * Security Note: PATH_TRAVERSAL_IN - URL comes from internal file storage after admin upload, not direct user
     * input. Access is controlled through admin-only upload permissions.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static ComponentHandler loadComponentHandler(
        URL url, Language language, String cacheKey, CacheManager cacheManager) {
        try {
            return switch (language) {
                case JAVA -> loadJavaComponentHandler(url, cacheKey, cacheManager);
                case JAVASCRIPT, PYTHON, RUBY -> loadPolyglotComponentHandler(url, language);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ComponentDefinition loadComponentDefinition(URL url, Language language) {
        ComponentDefinition componentDefinition;

        try {
            componentDefinition = switch (language) {
                case JAVA -> loadJavaComponentDefinition(url);
                case JAVASCRIPT, PYTHON, RUBY -> loadPolyglotComponentDefinition(url, language);
            };
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        if (componentDefinition == null) {
            throw new IllegalArgumentException("No component handler found in " + url);
        }

        return componentDefinition;
    }

    private static ComponentDefinition loadJavaComponentDefinition(URL url) {
        ComponentHandlerClassLoader componentHandlerClassLoader = ComponentHandlerClassLoader.of(
            url, url.toString(), null);

        return componentHandlerClassLoader.loadComponentDefinition();
    }

    private static ComponentHandler loadJavaComponentHandler(
        URL url, String cacheKey, CacheManager cacheManager) {
        ComponentHandlerClassLoader componentHandlerClassLoader =
            ComponentHandlerClassLoader.of(url, cacheKey, cacheManager);

        return componentHandlerClassLoader.loadComponentHandler();
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - URL comes from internal file storage after admin upload, not direct user
     * input.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private static ComponentHandler loadPolyglotComponentHandler(URL url, Language language)
        throws URISyntaxException, IOException {

        return ComponentHandlerPolyglotEngine.load(getLanguageId(language), Files.readString(Paths.get(url.toURI())));
    }

    private static ComponentDefinition loadPolyglotComponentDefinition(URL url, Language language)
        throws URISyntaxException, IOException {

        ComponentHandler componentHandler = loadPolyglotComponentHandler(url, language);

        return componentHandler.getDefinition();
    }

    private static String getLanguageId(Language language) {
        return switch (language) {
            case JAVASCRIPT -> "js";
            case PYTHON -> "python";
            case RUBY -> "ruby";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
}
