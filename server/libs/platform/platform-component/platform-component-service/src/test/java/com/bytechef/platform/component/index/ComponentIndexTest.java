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

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoaderKind;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
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

            ComponentIndex.Entry alphaEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "alpha".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(alphaEntry.title()).isEqualTo("Alpha (first)");
            assertThat(componentIndex.entries())
                .extracting(ComponentIndex.Entry::name)
                .containsExactlyInAnyOrder("alpha", "beta", "gamma");
            assertThat(componentIndex.entries())
                .extracting(ComponentIndex.Entry::loaderKind)
                .containsExactlyInAnyOrder(
                    ComponentHandlerLoaderKind.DEFAULT, ComponentHandlerLoaderKind.DEFAULT,
                    ComponentHandlerLoaderKind.JDBC);
        }
    }

    @Test
    public void testLoadReturnsEmptyOnUnknownLoaderKind() throws Exception {
        Path root = writeIndex("unknown", """
            {"entries":[
              {"name":"alpha","version":1,"providerClassName":"a.AlphaHandler","loaderKind":"bogus"}
            ]}""");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadReadsUnifiedApiCategoryAndTreatsMissingFieldAsNone() throws Exception {
        Path root = writeIndex("unified", """
            {"entries":[
              {"name":"alpha","version":1,"providerClassName":"a.AlphaHandler","loaderKind":"default",
               "unifiedApiCategory":"CRM"},
              {"name":"beta","version":1,"providerClassName":"b.BetaHandler","loaderKind":"default"}
            ]}""");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            ComponentIndex componentIndex = ComponentIndex.load(classLoader)
                .orElseThrow();

            ComponentIndex.Entry alphaEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "alpha".equals(entry.name()))
                .findFirst()
                .orElseThrow();
            ComponentIndex.Entry betaEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "beta".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(alphaEntry.unifiedApiCategory()).isEqualTo(UnifiedApiCategory.CRM);
            assertThat(betaEntry.unifiedApiCategory()).isNull();

            ComponentDefinition alphaStub = ComponentIndex.toStubComponentDefinition(alphaEntry);
            ComponentDefinition betaStub = ComponentIndex.toStubComponentDefinition(betaEntry);

            assertThat(alphaStub.getUnifiedApi()
                .map(UnifiedApiDefinition::getCategory)).contains(UnifiedApiCategory.CRM);
            assertThat(betaStub.getUnifiedApi()).isEmpty();
        }
    }

    @Test
    public void testLoadReturnsEmptyOnUnknownTriggerType() throws Exception {
        Path root = writeIndex("trigger", """
            {"entries":[
              {"name":"alpha","version":1,"providerClassName":"a.AlphaHandler","loaderKind":"default",
               "triggers":[{"name":"onEvent","type":"NOT_A_REAL_TRIGGER_TYPE"}]}
            ]}""");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadReturnsEmptyOnMissingProviderClassName() throws Exception {
        Path root = writeIndex("provider", """
            {"entries":[
              {"name":"alpha","version":1,"loaderKind":"default"}
            ]}""");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            assertThat(ComponentIndex.load(classLoader)).isEmpty();
        }
    }

    @Test
    public void testLoadReadsTriggerTypeAndDefaultsMissingListsToEmpty() throws Exception {
        Path root = writeIndex("lists", """
            {"entries":[
              {"name":"alpha","version":1,"providerClassName":"a.AlphaHandler","loaderKind":"default",
               "triggers":[{"name":"onEvent","type":"POLLING"}]}
            ]}""");

        try (URLClassLoader classLoader = classLoaderOf(root)) {
            ComponentIndex.Entry alphaEntry = ComponentIndex.load(classLoader)
                .orElseThrow()
                .entries()
                .getFirst();

            assertThat(alphaEntry.triggers()
                .getFirst()
                .type()).isEqualTo(TriggerType.POLLING);
            assertThat(alphaEntry.actions()).isEmpty();
            assertThat(alphaEntry.tags()).isEmpty();
            assertThat(alphaEntry.inputs()).isEmpty();
        }
    }
}
