/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.ComponentHandler;
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

    private static ComponentHandler loadJavaComponentHandler(
        URL url, String cacheKey, CacheManager cacheManager) throws IOException {

        try (ComponentHandlerClassLoader codeComponentHandlerClassLoader = ComponentHandlerClassLoader.of(
            url, cacheKey, cacheManager)) {

            return codeComponentHandlerClassLoader.loadComponentHandler();
        }
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
