/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerClassLoaderTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void testLoadComponentHandlerReadsDeclaredIcon() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();

        entries.put("assets/declared.svg", "<svg id=\"declared\"/>");
        entries.put("assets/sample.svg", "<svg id=\"sample\"/>");

        assertEquals("<svg id=\"declared\"/>", loadIcon(DeclaredIconComponentHandler.class, entries));
    }

    @Test
    void testLoadComponentHandlerFallsBackToComponentNamedIcon() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();

        entries.put("assets/noIcon.svg", "<svg id=\"named\"/>");
        entries.put("assets/sample.svg", "<svg id=\"sample\"/>");

        assertEquals("<svg id=\"named\"/>", loadIcon(NoIconComponentHandler.class, entries));
    }

    @Test
    void testLoadComponentHandlerFallsBackToSampleIcon() throws IOException {
        assertEquals(
            "<svg id=\"sample\"/>",
            loadIcon(DeclaredIconComponentHandler.class, Map.of("assets/sample.svg", "<svg id=\"sample\"/>")));
    }

    @Test
    void testLoadComponentHandlerWithoutAnyIconReturnsNoIcon() throws IOException {
        assertNull(loadIcon(NoIconComponentHandler.class, Map.of()));
    }

    @Test
    void testReadIconKeepsInlineIcon() {
        ComponentDefinition componentDefinition = ComponentDsl.component("inline")
            .icon("<svg id=\"inline\"/>");

        assertEquals(
            "<svg id=\"inline\"/>",
            ComponentHandlerClassLoader.readIcon(componentDefinition, getClass().getClassLoader()));
    }

    private String loadIcon(Class<? extends ComponentHandler> componentHandlerClass, Map<String, String> entries)
        throws IOException {

        Path jarPath = tempDirectory.resolve(componentHandlerClass.getSimpleName() + ".jar");

        try (OutputStream outputStream = Files.newOutputStream(jarPath);
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {

            jarOutputStream.putNextEntry(new JarEntry("META-INF/services/" + ComponentHandler.class.getName()));
            jarOutputStream.write(componentHandlerClass.getName()
                .getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
                jarOutputStream.write(entry.getValue()
                    .getBytes(StandardCharsets.UTF_8));
                jarOutputStream.closeEntry();
            }
        }

        try (ComponentHandlerClassLoader componentHandlerClassLoader = ComponentHandlerClassLoader.of(
            jarPath.toUri()
                .toURL(),
            componentHandlerClass.getName(), new ConcurrentMapCacheManager())) {

            ComponentHandler componentHandler = componentHandlerClassLoader.loadComponentHandler();

            ComponentDefinition componentDefinition = componentHandler.getDefinition();

            return componentDefinition.getIcon()
                .orElse(null);
        }
    }

    public static class DeclaredIconComponentHandler implements ComponentHandler {

        @Override
        public ComponentDefinition getDefinition() {
            return ComponentDsl.component("declaredIcon")
                .icon("path:assets/declared.svg");
        }
    }

    public static class NoIconComponentHandler implements ComponentHandler {

        @Override
        public ComponentDefinition getDefinition() {
            return ComponentDsl.component("noIcon");
        }
    }
}
