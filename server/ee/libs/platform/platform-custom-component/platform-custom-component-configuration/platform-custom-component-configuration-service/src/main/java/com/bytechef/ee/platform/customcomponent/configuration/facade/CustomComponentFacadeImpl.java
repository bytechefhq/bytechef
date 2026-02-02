/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class CustomComponentFacadeImpl implements CustomComponentFacade {

    private final CacheManager cacheManager;
    private final CustomComponentService customComponentService;
    private final CustomComponentFileStorage customComponentFileStorage;

    @SuppressFBWarnings("EI")
    public CustomComponentFacadeImpl(
        CacheManager cacheManager,
        CustomComponentService customComponentService, CustomComponentFileStorage customComponentFileStorage) {

        this.cacheManager = cacheManager;
        this.customComponentService = customComponentService;
        this.customComponentFileStorage = customComponentFileStorage;
    }

    @Override
    public void delete(Long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        customComponentService.delete(id);

        customComponentFileStorage.deleteCustomComponentFile(customComponent.getComponent());
    }

    @Transactional(readOnly = true)
    @Override
    public CustomComponentDefinitionRecord getCustomComponentDefinition(Long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        URL componentUrl = customComponentFileStorage.getCustomComponentFileURL(customComponent.getComponent());

        ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
            componentUrl, customComponent.getLanguage(),
            componentUrl.toString() + UUID.randomUUID(), cacheManager);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        List<ActionDefinitionRecord> actions = componentDefinition.getActions()
            .orElse(Collections.emptyList())
            .stream()
            .map(this::toActionDefinitionRecord)
            .toList();

        List<TriggerDefinitionRecord> triggers = componentDefinition.getTriggers()
            .orElse(Collections.emptyList())
            .stream()
            .map(this::toTriggerDefinitionRecord)
            .toList();

        return new CustomComponentDefinitionRecord(actions, triggers);
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
        customComponent.setComponent(componentFileEntry);
        customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setEnabled(true);
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setName(componentDefinition.getName());
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));
        customComponent.setLanguage(language);

        customComponentService.create(customComponent);
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - Temporary files are created with system-generated names in the temp directory,
     * not user-controlled paths. Access is restricted to administrators.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private ComponentDefinition loadComponentDefinition(Language language, byte[] bytes) throws IOException {
        Path path = Files.createTempFile("custom_component", language.getExtension());

        Files.write(path, bytes);

        URI uri = path.toUri();

        try {
            ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
                uri.toURL(), language, uri.toString() + UUID.randomUUID(), cacheManager);

            return componentHandler.getDefinition();
        } finally {
            Files.delete(path);
        }
    }

    private ActionDefinitionRecord toActionDefinitionRecord(ActionDefinition actionDefinition) {
        return new ActionDefinitionRecord(
            actionDefinition.getName(),
            OptionalUtils.orElse(actionDefinition.getTitle(), null),
            OptionalUtils.orElse(actionDefinition.getDescription(), null));
    }

    private TriggerDefinitionRecord toTriggerDefinitionRecord(TriggerDefinition triggerDefinition) {
        return new TriggerDefinitionRecord(
            triggerDefinition.getName(),
            OptionalUtils.orElse(triggerDefinition.getTitle(), null),
            OptionalUtils.orElse(triggerDefinition.getDescription(), null));
    }

    private void update(CustomComponent customComponent, ComponentDefinition componentDefinition) {
        customComponent.setDescription(
            OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));

        customComponentService.update(customComponent);
    }
}
