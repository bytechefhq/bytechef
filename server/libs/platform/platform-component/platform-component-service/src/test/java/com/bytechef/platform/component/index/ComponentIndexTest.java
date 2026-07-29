/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
public class ComponentIndexTest {

    @TempDir
    Path tempDir;

    private Path writeIndex(String directoryName, String json) throws Exception {
        Path directoryPath = tempDir.resolve(directoryName);

        Path indexPath = directoryPath.resolve(ComponentIndex.RESOURCE_PATH);

        Files.createDirectories(Objects.requireNonNull(indexPath.getParent()));
        Files.writeString(indexPath, json);

        return directoryPath;
    }

    private static URLClassLoader classLoaderOf(Path... roots) throws Exception {
        URL[] urls = new URL[roots.length];

        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toUri()
                .toURL();
        }

        return new URLClassLoader(urls, null);
    }

    @Test
    public void testLoadReturnsEmptyWhenNoResourceExists() throws Exception {
        try (URLClassLoader classLoader = classLoaderOf(tempDir)) {
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadReturnsEmptyOnMalformedJson() throws Exception {
        Path root = writeIndex("broken", "this is not json");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            // Broken index must degrade to empty (registry then falls back to full loading), never throw.
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadMergesMultipleResourcesAndDeduplicatesByNameAndVersion() throws Exception {
        String first = """
            {"entries":[
              {"name":"alpha","version":1,"title":"Alpha (first)","providerClassName":"a.AlphaHandler",
               "loaderKind":"default"},
              {"name":"beta","version":1,"title":"Beta","providerClassName":"b.BetaHandler","loaderKind":"default"}
            ]}""";
        String second = """
            {"entries":[
              {"name":"alpha","version":1,"title":"Alpha (second)","providerClassName":"a.AlphaHandler",
               "loaderKind":"default"},
              {"name":"gamma","version":2,"title":"Gamma","providerClassName":"g.GammaHandler","loaderKind":"jdbc"}
            ]}""";

        Path firstRoot = writeIndex("first", first);
        Path secondRoot = writeIndex("second", second);

        try (URLClassLoader classLoader = classLoaderOf(firstRoot, secondRoot)) {
            Optional<ComponentIndex> componentIndexOptional = ComponentIndex.load(classLoader);

            assertThat(componentIndexOptional).isPresent();

            ComponentIndex componentIndex = componentIndexOptional.orElseThrow();

            assertThat(componentIndex.entries()).hasSize(3);

            // Duplicate name/version pairs keep the first occurrence.
            ComponentIndex.Entry alphaEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "alpha".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(alphaEntry.title()).isEqualTo("Alpha (first)");
            assertThat(componentIndex.entries())
                .extracting(ComponentIndex.Entry::name)
                .containsExactlyInAnyOrder("alpha", "beta", "gamma");
        }
    }
}
