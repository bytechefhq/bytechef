/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.loader.AbstractHandlerLoader;
import com.bytechef.embedded.integration.IntegrationHandler;
import java.io.IOException;
import java.net.URL;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationHandlerLoader extends AbstractHandlerLoader<IntegrationHandler> {

    private static final IntegrationHandlerLoader INSTANCE = new IntegrationHandlerLoader();

    private IntegrationHandlerLoader() {
    }

    public static IntegrationHandler
        loadIntegrationHandler(URL url, Language language, String cacheKey, CacheManager cacheManager) {
        return INSTANCE.loadHandler(url, language, cacheKey, cacheManager);
    }

    @Override
    protected IntegrationHandler loadJavaHandler(URL url, String cacheKey, CacheManager cacheManager)
        throws IOException {
        try (IntegrationHandlerClassLoader integrationHandlerClassLoader =
            IntegrationHandlerClassLoader.of(url, cacheKey, cacheManager)) {
            return integrationHandlerClassLoader.loadHandler();
        }
    }

    @Override
    protected IntegrationHandler loadPolyglotHandler(String languageId, String script) {
        return IntegrationHandlerPolyglotEngine.load(languageId, script);
    }
}
