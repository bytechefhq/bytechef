/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.example.customcomponent.LazyHelper;
import com.example.customcomponent.LazyHelperComponentHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ComponentHandlerLoaderTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void testLoadComponentHandlerKeepsJarReadableForClassesFirstUsedAtPerform() throws Exception {
        ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
            writeJar(), Language.JAVA, "lazyHelper", new ConcurrentMapCacheManager());

        assertEquals("Hello ByteChef", performGreet(componentHandler));
    }

    @Test
    void testLoadComponentHandlerTwiceReturnsCachedHandlerThatStillPerforms() throws Exception {
        CacheManager cacheManager = new ConcurrentMapCacheManager();
        URL jarUrl = writeJar();

        ComponentHandler firstComponentHandler = ComponentHandlerLoader.loadComponentHandler(
            jarUrl, Language.JAVA, "lazyHelper", cacheManager);
        ComponentHandler secondComponentHandler = ComponentHandlerLoader.loadComponentHandler(
            jarUrl, Language.JAVA, "lazyHelper", cacheManager);

        assertSame(firstComponentHandler, secondComponentHandler);
        assertEquals("Hello ByteChef", performGreet(secondComponentHandler));
    }

    @Test
    void testLoadComponentDefinitionClosesItsClassLoader() throws Exception {
        ComponentDefinition componentDefinition = ComponentHandlerLoader.loadComponentDefinition(
            writeJar(), Language.JAVA);

        List<ActionDefinition> actionDefinitions = componentDefinition.getActions();

        ActionDefinition actionDefinition = actionDefinitions.getFirst();

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        Class<?> performFunctionClass = performFunction.getClass();

        ClassLoader classLoader = performFunctionClass.getClassLoader();

        String lazyHelperResource = LazyHelper.class.getName()
            .replace('.', '/') + ".class";

        assertEquals("lazyHelper", componentDefinition.getName());
        assertNull(classLoader.getResource(lazyHelperResource));
    }

    private static Object performGreet(ComponentHandler componentHandler) throws Exception {
        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        List<ActionDefinition> actionDefinitions = componentDefinition.getActions();

        ActionDefinition actionDefinition = actionDefinitions.getFirst();

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        return performFunction.apply(null, null, null);
    }

    private URL writeJar() throws IOException {
        Path jarPath = tempDirectory.resolve("lazy-helper.jar");

        try (OutputStream outputStream = Files.newOutputStream(jarPath);
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {

            jarOutputStream.putNextEntry(new JarEntry("META-INF/services/" + ComponentHandler.class.getName()));
            jarOutputStream.write(LazyHelperComponentHandler.class.getName()
                .getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();

            for (Class<?> fixtureClass : List.of(LazyHelperComponentHandler.class, LazyHelper.class)) {
                String entryName = fixtureClass.getName()
                    .replace('.', '/') + ".class";

                jarOutputStream.putNextEntry(new JarEntry(entryName));

                try (InputStream inputStream = fixtureClass.getClassLoader()
                    .getResourceAsStream(entryName)) {

                    inputStream.transferTo(jarOutputStream);
                }

                jarOutputStream.closeEntry();
            }
        }

        return jarPath.toUri()
            .toURL();
    }
}
