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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Component.CustomComponent.JavaLoader;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.exception.CustomComponentErrorType;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final boolean javaEnabled;
    private final ComponentHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public CustomComponentFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager,
        CustomComponentService customComponentService, CustomComponentFileStorage customComponentFileStorage) {

        this.cacheManager = cacheManager;
        this.customComponentService = customComponentService;
        this.customComponentFileStorage = customComponentFileStorage;
        this.javaEnabled = applicationProperties.getComponent()
            .getCustomComponent()
            .isJavaEnabled();
        this.javaLoader = toLoaderJavaLoader(applicationProperties);
    }

    private static ComponentHandlerLoader.JavaLoader toLoaderJavaLoader(ApplicationProperties applicationProperties) {
        ApplicationProperties.Component component = applicationProperties.getComponent();

        ApplicationProperties.Component.CustomComponent customComponent = component.getCustomComponent();

        return customComponent.getJavaLoader() == JavaLoader.CLASS_LOADER
            ? ComponentHandlerLoader.JavaLoader.CLASS_LOADER
            : ComponentHandlerLoader.JavaLoader.ESPRESSO;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent createEmptyCustomComponent(String name, Language language) {
        if (name == null || name.isBlank() || name.indexOf('"') >= 0 || name.indexOf('\\') >= 0
            || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            throw new ConfigurationException(
                "Invalid component name: must not be blank or contain quotes, backslashes, or newlines",
                CustomComponentErrorType.INVALID_COMPONENT_NAME);
        }

        if (language != Language.JAVASCRIPT) {
            throw new ConfigurationException(
                "Create-empty currently supports JavaScript only",
                CustomComponentErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        if (customComponentService.fetchCustomComponent(name, 1)
            .isPresent()) {
            throw new ConfigurationException(
                "A custom component named '" + name + "' already exists",
                CustomComponentErrorType.COMPONENT_ALREADY_EXISTS);
        }

        String template = readTemplate(language).replace("__NAME__", name);

        byte[] bytes = template.getBytes(StandardCharsets.UTF_8);

        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
                componentDefinition.getName() + "_" + componentDefinition.getVersion() + "." + language.getExtension(),
                bytes);

            return create(
                language, componentDefinition, componentDefinition.getVersion(), componentFileEntry,
                CustomComponent.Status.DRAFT, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(Long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        customComponentService.delete(id);

        customComponentFileStorage.deleteCustomComponentFile(customComponent.getComponent());
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponentDefinitionRecord getCustomComponentDefinition(Long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        URL componentUrl = customComponentFileStorage.getCustomComponentFileURL(customComponent.getComponent());

        ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
            componentUrl, customComponent.getLanguage(), javaLoader,
            componentUrl.toString() + UUID.randomUUID(), cacheManager);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        List<ActionDefinitionRecord> actions = componentDefinition.getActions()
            .stream()
            .map(this::toActionDefinitionRecord)
            .toList();

        List<TriggerDefinitionRecord> triggers = componentDefinition.getTriggers()
            .stream()
            .map(this::toTriggerDefinitionRecord)
            .toList();

        return new CustomComponentDefinitionRecord(actions, triggers);
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<CustomComponent> getCustomComponents() {
        return customComponentService.getCustomComponents();
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String getCustomComponentSource(long id) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        if (customComponent.getLanguage() == Language.JAVA) {
            throw new ConfigurationException(
                "Java custom components have no editable source", CustomComponentErrorType.JAVA_SOURCE_NOT_EDITABLE);
        }

        return customComponentFileStorage.readCustomComponentFileContent(customComponent.getComponent());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void save(byte[] bytes, Language language) {
        if (!javaEnabled && language == Language.JAVA) {
            throw new ConfigurationException(
                "Uploading of Java custom components is disabled",
                CustomComponentErrorType.JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED);
        }

        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            int version = componentDefinition.getVersion();

            Optional<CustomComponent> existingCustomComponent = customComponentService.fetchCustomComponent(
                componentDefinition.getName(), version);

            existingCustomComponent
                .filter(customComponent -> customComponent.getStatus() == CustomComponent.Status.DRAFT)
                .ifPresent(customComponent -> {
                    throw new ConfigurationException(
                        "Version " + version + " of '" + componentDefinition.getName() +
                            "' is owned by a draft; publish or delete the draft first",
                        CustomComponentErrorType.VERSION_ALREADY_EXISTS);
                });

            FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
                componentDefinition.getName() + "_" + version + "." + language.getExtension(), bytes);

            existingCustomComponent.ifPresentOrElse(
                customComponent -> updateUploaded(customComponent, componentDefinition, componentFileEntry),
                () -> create(
                    language, componentDefinition, version, componentFileEntry, CustomComponent.Status.PUBLISHED,
                    Instant.now()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent publishCustomComponent(long id) {
        return customComponentService.publishCustomComponent(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public CustomComponent updateCustomComponentSource(long id, String content) {
        CustomComponent customComponent = customComponentService.getCustomComponent(id);

        Language language = customComponent.getLanguage();

        if (language == Language.JAVA) {
            throw new ConfigurationException(
                "Java custom components have no editable source", CustomComponentErrorType.JAVA_SOURCE_NOT_EDITABLE);
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        try {
            ComponentDefinition componentDefinition = loadComponentDefinition(language, bytes);

            if (!Objects.equals(componentDefinition.getName(), customComponent.getName())) {
                throw new ConfigurationException(
                    "Renaming a component by editing its source is not supported (expected name '"
                        + customComponent.getName() + "')",
                    CustomComponentErrorType.SOURCE_RENAME_UNSUPPORTED);
            }

            if (customComponent.getStatus() == CustomComponent.Status.DRAFT) {
                return updateDraft(customComponent, componentDefinition, language, bytes);
            }

            return createDraftFromPublished(customComponent, componentDefinition, language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CustomComponent updateDraft(
        CustomComponent customComponent, ComponentDefinition componentDefinition, Language language, byte[] bytes) {

        int newComponentVersion = componentDefinition.getVersion();

        if (newComponentVersion != customComponent.getComponentVersion()) {
            customComponentService.fetchCustomComponent(customComponent.getName(), newComponentVersion)
                .filter(existing -> !Objects.equals(existing.getId(), customComponent.getId()))
                .ifPresent(existing -> {
                    throw new ConfigurationException(
                        "Version " + newComponentVersion + " of component '" + customComponent.getName() +
                            "' already exists",
                        CustomComponentErrorType.VERSION_ALREADY_EXISTS);
                });
        }

        FileEntry oldComponentFileEntry = customComponent.getComponent();

        FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
            componentDefinition.getName() + "_" + newComponentVersion + "." + language.getExtension(), bytes);

        customComponent.setComponent(componentFileEntry);
        customComponent.setComponentVersion(newComponentVersion);
        customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));

        CustomComponent savedCustomComponent = customComponentService.update(customComponent);

        if (oldComponentFileEntry != null &&
            !Objects.equals(oldComponentFileEntry.getUrl(), componentFileEntry.getUrl())) {

            customComponentFileStorage.deleteCustomComponentFile(oldComponentFileEntry);
        }

        return savedCustomComponent;
    }

    private CustomComponent createDraftFromPublished(
        CustomComponent publishedCustomComponent, ComponentDefinition componentDefinition, Language language,
        byte[] bytes) {

        String name = publishedCustomComponent.getName();

        customComponentService.fetchDraftCustomComponent(name)
            .ifPresent(draft -> {
                throw new ConfigurationException(
                    "A draft (version " + draft.getComponentVersion() + ") of component '" + name +
                        "' already exists; edit that draft instead",
                    CustomComponentErrorType.DRAFT_ALREADY_EXISTS);
            });

        int latestComponentVersion = customComponentService.fetchLatestCustomComponent(name)
            .map(CustomComponent::getComponentVersion)
            .orElse(0);

        if (componentDefinition.getVersion() <= latestComponentVersion) {
            throw new ConfigurationException(
                "Editing published version " + publishedCustomComponent.getComponentVersion() +
                    " requires raising the version declared in the source above " + latestComponentVersion +
                    " to start a new draft",
                CustomComponentErrorType.VERSION_NOT_BUMPED);
        }

        FileEntry componentFileEntry = customComponentFileStorage.storeCustomComponentFile(
            componentDefinition.getName() + "_" + componentDefinition.getVersion() + "." + language.getExtension(),
            bytes);

        return create(language, componentDefinition, componentDefinition.getVersion(), componentFileEntry,
            CustomComponent.Status.DRAFT, null);
    }

    private CustomComponent create(
        Language language, ComponentDefinition componentDefinition, int componentVersion,
        FileEntry componentFileEntry, CustomComponent.Status status, Instant publishedDate) {

        CustomComponent customComponent = new CustomComponent();

        customComponent.setComponentVersion(componentVersion);
        customComponent.setComponent(componentFileEntry);
        customComponent.setDescription(OptionalUtils.orElse(componentDefinition.getDescription(), null));
        customComponent.setEnabled(true);
        customComponent.setIcon(OptionalUtils.orElse(componentDefinition.getIcon(), null));
        customComponent.setName(componentDefinition.getName());
        customComponent.setPublishedDate(publishedDate);
        customComponent.setStatus(status);
        customComponent.setTitle(OptionalUtils.orElse(componentDefinition.getTitle(), null));
        customComponent.setLanguage(language);

        return customComponentService.create(customComponent);
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
                uri.toURL(), language, javaLoader, uri.toString() + UUID.randomUUID(), cacheManager);

            return componentHandler.getDefinition();
        } finally {
            Files.delete(path);
        }
    }

    private static String readTemplate(Language language) {
        String resource = "custom-component-templates/starter." + language.getExtension();

        try (InputStream inputStream =
            CustomComponentFacadeImpl.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private void updateUploaded(
        CustomComponent customComponent, ComponentDefinition componentDefinition, FileEntry componentFileEntry) {

        customComponent.setComponent(componentFileEntry);

        update(customComponent, componentDefinition);
    }
}
