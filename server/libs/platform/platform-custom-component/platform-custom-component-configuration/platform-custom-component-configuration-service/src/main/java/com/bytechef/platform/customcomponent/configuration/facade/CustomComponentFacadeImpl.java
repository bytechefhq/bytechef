/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.customcomponent.configuration.facade;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.platform.customcomponent.loader.ComponentHandlerLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class CustomComponentFacadeImpl implements CustomComponentFacade {

    private final CustomComponentService customComponentService;
    private final CustomComponentFileStorage customComponentFileStorage;

    @SuppressFBWarnings("EI")
    public CustomComponentFacadeImpl(
        CustomComponentService customComponentService, CustomComponentFileStorage customComponentFileStorage) {

        this.customComponentService = customComponentService;
        this.customComponentFileStorage = customComponentFileStorage;
    }

    @Override
    public void delete(Long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        customComponentService.delete(id);

        customComponentFileStorage.deleteCustomComponentFile(customComponent.getComponentFile());
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomComponent> getCustomComponents() {
        return customComponentService.getCustomComponents();
    }

    @Override
    public void save(byte[] bytes, Language language) {
        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
                componentDefinition.getName() + "_" + componentDefinition.getVersion() + "." + language.getExtension(),
                bytes);

            customComponentService.fetchCustomComponent(componentDefinition.getName(), componentDefinition.getVersion())
                .ifPresentOrElse(
                    customComponent -> update(customComponent, componentDefinition),
                    () -> create(language, componentDefinition, componentDefinition.getVersion(), componentFileEntry));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void create(
        Language language, ComponentDefinition componentDefinition, int componentVersion,
        FileEntry componentFileEntry) {

        CustomComponent customComponent = new CustomComponent();

        customComponent.setComponentVersion(componentVersion);
        customComponent.setComponentFile(componentFileEntry);
        customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setEnabled(true);
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setName(componentDefinition.getName());
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));
        customComponent.setLanguage(language);

        customComponentService.create(customComponent);
    }

    private ComponentDefinition loadComponentDefinition(Language language, byte[] bytes) throws IOException {
        Path path = Files.createTempFile("custom_component", language.getExtension());

        Files.write(path, bytes);

        URI uri = path.toUri();

        try {
            ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
                uri.toURL(), language, uri.toString() + UUID.randomUUID());

            return componentHandler.getDefinition();
        } finally {
            Files.delete(path);
        }
    }

    private void update(CustomComponent customComponent, ComponentDefinition componentDefinition) {
        customComponent.setDescription(
            OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));

        customComponentService.update(customComponent);
    }
}
