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
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.slack.SlackComponentHandler;
import com.bytechef.component.xero.XeroComponentHandler;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoaderKind;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class ComponentIndexGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testGeneratorRoundTrip() throws Exception {
        Path indexPath = tempDir.resolve("META-INF/bytechef/component-index.json");

        ComponentIndexGenerator.main(new String[] {
            indexPath.toString()
        });

        try (URLClassLoader classLoader = new URLClassLoader(
            new URL[] {
                tempDir.toUri()
                    .toURL()
            }, null)) {

            Optional<ComponentIndex> componentIndexOptional = ComponentIndex.load(classLoader);

            assertThat(componentIndexOptional).isPresent();

            ComponentIndex componentIndex = componentIndexOptional.orElseThrow();

            ComponentIndex.Entry slackEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "slack".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(slackEntry.providerClassName()).isEqualTo(SlackComponentHandler.class.getName());
            assertThat(slackEntry.loaderKind()).isEqualTo(ComponentHandlerLoaderKind.DEFAULT);
            assertThat(slackEntry.actions()).isNotEmpty();
            assertThat(slackEntry.connection()).isNotNull();
            assertThat(slackEntry.unifiedApiCategory()).isNull();

            ComponentIndex.Entry xeroEntry = componentIndex.entries()
                .stream()
                .filter(entry -> "xero".equals(entry.name()))
                .findFirst()
                .orElseThrow();

            assertThat(xeroEntry.unifiedApiCategory()).isEqualTo(UnifiedApiCategory.ACCOUNTING);

            ComponentDefinition xeroStub = ComponentIndex.toStubComponentDefinition(xeroEntry);

            com.bytechef.platform.component.domain.ComponentDefinition xeroStubDomainComponentDefinition =
                new com.bytechef.platform.component.domain.ComponentDefinition(xeroStub);
            com.bytechef.platform.component.domain.ComponentDefinition xeroFullDomainComponentDefinition =
                new com.bytechef.platform.component.domain.ComponentDefinition(
                    new XeroComponentHandler().getDefinition());

            assertThat(xeroStubDomainComponentDefinition.getUnifiedApiCategory())
                .isEqualTo(xeroFullDomainComponentDefinition.getUnifiedApiCategory())
                .isEqualTo(UnifiedApiCategory.ACCOUNTING);
        }

        assertThat(Files.readString(indexPath)).contains("\"slack\"");
    }
}
