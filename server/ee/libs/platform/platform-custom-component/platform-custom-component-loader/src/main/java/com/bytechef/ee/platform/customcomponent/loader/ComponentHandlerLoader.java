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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentHandlerLoader {

    /**
     * How Java custom component jars are loaded: inside a sandboxed GraalVM Espresso guest JVM or in-process via an
     * isolating classloader.
     */
    public enum JavaLoader {
        CLASS_LOADER, ESPRESSO
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - URL comes from internal file storage after admin upload, not direct user
     * input. Access is controlled through admin-only upload permissions.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static ComponentHandler loadComponentHandler(
        URL url, Language language, JavaLoader javaLoader, String cacheKey, CacheManager cacheManager) {

        try {
            return switch (language) {
                case JAVA -> javaLoader == JavaLoader.ESPRESSO
                    ? ComponentHandlerEspressoEngine.load(toLocalPath(url))
                    : loadJavaComponentHandler(url, cacheKey, cacheManager);
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
            case JAVASCRIPT -> "js";
            case PYTHON -> "python";
            case RUBY -> "ruby";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    /**
     * <b>Security Note:</b> Path traversal and the URL fetch are intentional. The URL is derived from internal custom
     * component file storage, not from untrusted user input.
     */
    @SuppressFBWarnings({
        "PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"
    })
    private static Path toLocalPath(URL url) throws IOException, URISyntaxException {
        URI uri = url.toURI();

        if ("file".equals(uri.getScheme())) {
            return Paths.get(uri);
        }

        Path tempFile = Files.createTempFile("custom_component", null);

        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }
}
