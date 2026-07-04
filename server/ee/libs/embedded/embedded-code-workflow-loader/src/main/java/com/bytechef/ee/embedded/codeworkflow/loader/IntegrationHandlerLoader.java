/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.embedded.integration.IntegrationHandler;
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
public class IntegrationHandlerLoader {

    /**
     * How Java code workflow jars are loaded: in-process via an isolating classloader or inside a sandboxed GraalVM
     * Espresso guest JVM.
     */
    public enum JavaLoader {
        CLASS_LOADER, ESPRESSO
    }

    /**
     * <b>Security Note:</b> Path traversal is intentional. The URL is derived from internal code workflow container
     * configuration, not from untrusted user input.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static IntegrationHandler loadIntegrationHandler(
        URL url, Language language, JavaLoader javaLoader, String cacheKey, CacheManager cacheManager) {

        try {
            return switch (language) {
                case JAVA -> javaLoader == JavaLoader.ESPRESSO
                    ? IntegrationHandlerPolyglotEngine.loadJava(toLocalPath(url))
                    : loadJavaIntegrationHandler(url, cacheKey, cacheManager);
                case JAVASCRIPT, PYTHON, RUBY -> IntegrationHandlerPolyglotEngine.load(
                    getLanguageId(language), Files.readString(toLocalPath(url)));
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static IntegrationHandler loadJavaIntegrationHandler(URL url, String cacheKey, CacheManager cacheManager)
        throws IOException {

        try (IntegrationHandlerClassLoader integrationHandlerClassLoader = IntegrationHandlerClassLoader.of(
            url, cacheKey, cacheManager)) {

            return integrationHandlerClassLoader.loadIntegrationHandler();
        }
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
     * <b>Security Note:</b> Path traversal and the URL fetch are intentional. The URL is derived from internal code
     * workflow container configuration, not from untrusted user input.
     */
    @SuppressFBWarnings({
        "PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"
    })
    private static Path toLocalPath(URL url) throws IOException, URISyntaxException {
        URI uri = url.toURI();

        if ("file".equals(uri.getScheme())) {
            return Paths.get(uri);
        }

        Path tempFile = Files.createTempFile("code_workflow", null);

        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }
}
