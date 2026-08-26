/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Workflow.CodeWorkflow;
import com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoader;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deploys code-backed integrations for the embedded surface. The embedded mirror of the automation
 * {@code ProjectCodeWorkflowFacadeImpl}: integrations are keyed globally by component name rather than scoped to a
 * workspace.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationCodeWorkflowFacadeImpl implements IntegrationCodeWorkflowFacade {

    private final CacheManager cacheManager;
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final IntegrationCodeWorkflowService integrationCodeWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final TagService tagService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final boolean javaEnabled;
    private final IntegrationHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public IntegrationCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        IntegrationCodeWorkflowService integrationCodeWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowFileStorage codeWorkflowFileStorage, TagService tagService, WorkflowService workflowService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.cacheManager = cacheManager;
        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.integrationCodeWorkflowService = integrationCodeWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.javaEnabled = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .isJavaEnabled();
        this.javaLoader = toLoaderJavaLoader(applicationProperties);
    }

    private static IntegrationHandlerLoader.JavaLoader toLoaderJavaLoader(ApplicationProperties applicationProperties) {
        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();

        CodeWorkflow codeWorkflow = workflow.getCodeWorkflow();

        return codeWorkflow.getJavaLoader() == CodeWorkflow.JavaLoader.ESPRESSO
            ? IntegrationHandlerLoader.JavaLoader.ESPRESSO
            : IntegrationHandlerLoader.JavaLoader.CLASS_LOADER;
    }

    /**
     * Creates a code-backed integration from scratch by rendering the language starter template (substituting the
     * requested component name), creating the integration, and saving the rendered script as a draft (never publishing
     * it). Restricted to administrators, mirroring {@link #save}, because loading the rendered script runs it on the
     * server.
     *
     * <p>
     * A component may back several integrations, told apart by name, so a second one on the same component is allowed —
     * this method always creates, never reuses or overwrites an existing integration.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Integration createEmptyCodeWorkflow(String componentName, Language language) {
        return createEmptyCodeWorkflow(componentName, language, null, null, null, null, null);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Integration createEmptyCodeWorkflow(
        String componentName, Language language, @Nullable String name, @Nullable String description,
        @Nullable Long categoryId, @Nullable List<String> tags, @Nullable String permissionExpression) {
        if (componentName == null || componentName.isBlank() || componentName.indexOf('"') >= 0
            || componentName.indexOf('\\') >= 0 || componentName.indexOf('\n') >= 0
            || componentName.indexOf('\r') >= 0) {

            throw new ConfigurationException(
                "Invalid code workflow name: must not be blank or contain quotes, backslashes, or newlines",
                CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
        }

        // RUBY-DISABLED: RUBY is dropped from the create-empty allowlist because org.graalvm.polyglot:ruby is
        // published only up to 25.0.0 and crashes on the Truffle 25.2.4 this repo pins, so the rendered Ruby starter
        // template could not be loaded by the polyglot loader below. A RUBY request is rejected with
        // LANGUAGE_NOT_SUPPORTED — the same contract JAVA already gets — rather than silently downgraded to another
        // language. The Language.RUBY constant and the Ruby starter template are untouched (ordinals are persisted as
        // INTs). Restore RUBY here, and in the message, once a polyglot ruby jar built on Truffle 25.2+ is published
        // (or GraalVM is downgraded). Grep RUBY-DISABLED.
        if (language != Language.JAVASCRIPT && language != Language.PYTHON) {
//        if (language != Language.JAVASCRIPT && language != Language.PYTHON && language != Language.RUBY) {
            throw new ConfigurationException(
                "Create-empty supports JavaScript and Python only",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        String template = readTemplate(language).replace("__NAME__", componentName);

        byte[] bytes = template.getBytes(StandardCharsets.UTF_8);

        IntegrationDefinition integrationDefinition;

        try {
            integrationDefinition = loadIntegrationDefinition(language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Integration integration = createIntegration(
            integrationDefinition, name, description, categoryId, tags, permissionExpression);

        List<WorkflowDefinition> workflowDefinitions = integrationDefinition.getWorkflows()
            .orElseGet(List::of);

        saveDraft(integration, integrationDefinition, workflowDefinitions, bytes, language);

        return integration;
    }

    /**
     * Returns every integration that has at least one code workflow deployed, resolved from the distinct integration
     * ids recorded on {@code integration_code_workflow}.
     */
    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<Integration> getCodeWorkflowIntegrations() {
        List<Long> integrationIds = integrationCodeWorkflowService.getCodeWorkflowIntegrationIds();

        if (integrationIds.isEmpty()) {
            return List.of();
        }

        return integrationService.getIntegrations(integrationIds);
    }

    /**
     * Returns the language of the code workflow backing {@code integrationId}, if one exists. Mirrors the resolution
     * chain {@code IntegrationFacadeImpl#toIntegrationDTO} uses to surface the language on integration DTOs, but
     * exposed standalone here so callers that only need the language (rather than the whole DTO) are not forced to pull
     * in the wider integration facade.
     */
    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Optional<String> getCodeWorkflowLanguage(long integrationId) {
        return integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(integrationId)
            .flatMap(this::fetchCodeWorkflowContainer)
            .map(codeWorkflowContainer -> codeWorkflowContainer.getLanguage()
                .name());
    }

    /**
     * Returns the stored source text of the code workflow backing {@code integrationId}, so it can be shown in an
     * editor. Java-backed containers have no editable source (they are compiled jars), so those are rejected.
     */
    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String getCodeWorkflowSource(long integrationId) {
        CodeWorkflowContainer codeWorkflowContainer = getCodeWorkflowContainer(integrationId);

        if (codeWorkflowContainer.getLanguage() == Language.JAVA) {
            throw new ConfigurationException(
                "Java code workflows have no editable source", CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        return codeWorkflowFileStorage.readCodeWorkflowFileContent(codeWorkflowContainer.getWorkflows());
    }

    /**
     * Deploying a code workflow loads and executes the uploaded artifact (a JAR or polyglot script) on the server, so
     * it is restricted to administrators. The guard lives here on the facade so it protects every caller, not only the
     * REST entry point.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void save(byte[] bytes, Language language) {
        // RUBY-DISABLED: a Ruby upload is rejected here as well as at create-empty, because deploying one would
        // store a workflow the polyglot loader cannot run — org.graalvm.polyglot:ruby is published only up to 25.0.0,
        // which crashes on the Truffle 25.2.4 this repo pins. Without this the REST/API caller kept a path the client
        // no longer offers: the accept lists dropped .rb, but nothing server-side refused it. Restore by deleting this
        // block once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is downgraded). Grep
        // RUBY-DISABLED.
        if (language == Language.RUBY) {
            throw new ConfigurationException(
                "Uploading of Ruby code workflows is temporarily disabled",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        if (!javaEnabled && language == Language.JAVA) {
            throw new ConfigurationException(
                "Uploading of Java code workflows is disabled",
                CodeWorkflowErrorType.JAVA_CODE_WORKFLOW_UPLOAD_DISABLED);
        }

        IntegrationDefinition integrationDefinition;

        try {
            integrationDefinition = loadIntegrationDefinition(language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Integration integration = resolveUploadTarget(integrationDefinition);

        deployInto(integration, integrationDefinition, bytes, language);
    }

    /**
     * Re-deploys new source onto an already-resolved integration rather than resolving the target integration by
     * component name (as {@link #save} does for uploads). Renaming an integration's component by editing its source is
     * not supported, so the incoming {@link IntegrationDefinition#getComponentName()} must match the integration's
     * current component name.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateCodeWorkflowSource(long integrationId, String content) {
        CodeWorkflowContainer codeWorkflowContainer = getCodeWorkflowContainer(integrationId);

        Language language = codeWorkflowContainer.getLanguage();

        if (language == Language.JAVA) {
            throw new ConfigurationException(
                "Java code workflows have no editable source", CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        Integration integration = integrationService.getIntegration(integrationId);

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        IntegrationDefinition integrationDefinition;

        try {
            integrationDefinition = loadIntegrationDefinition(language, bytes);
        } catch (Exception e) {
            throw new ConfigurationException(
                "Failed to load code workflow source: " + e.getMessage(), CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
        }

        if (!Objects.equals(integrationDefinition.getComponentName(), integration.getComponentName())) {
            throw new ConfigurationException(
                "Renaming a code workflow by editing its source is not supported (expected component name '"
                    + integration.getComponentName() + "')",
                CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);
        }

        List<WorkflowDefinition> workflowDefinitions = integrationDefinition.getWorkflows()
            .orElseGet(List::of);

        saveDraft(integration, integrationDefinition, workflowDefinitions, bytes, language);
    }

    private void saveDraft(
        Integration integration, IntegrationDefinition integrationDefinition,
        List<WorkflowDefinition> workflowDefinitions, byte[] bytes, Language language) {

        long integrationId = Objects.requireNonNull(integration.getId());
        int draftIntegrationVersion = integration.getLastIntegrationVersion();

        IntegrationCodeWorkflow latestIntegrationCodeWorkflow = integrationCodeWorkflowService
            .fetchIntegrationCodeWorkflow(integrationId)
            .orElse(null);

        CodeWorkflowContainerFacade.CodeWorkflowReconciliation reconciliation;

        if (latestIntegrationCodeWorkflow != null
            && latestIntegrationCodeWorkflow.getIntegrationVersion() == draftIntegrationVersion) {

            CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
                latestIntegrationCodeWorkflow.getCodeWorkflowContainerId());

            reconciliation = codeWorkflowContainerFacade.update(
                codeWorkflowContainer, integrationDefinition.getVersion(), workflowDefinitions, bytes,
                PlatformType.EMBEDDED);
        } else {
            Map<String, String> reusableWorkflowNameIds = latestIntegrationCodeWorkflow == null
                ? Map.of()
                : resolveDraftWorkflowNameIds(latestIntegrationCodeWorkflow, integrationId, draftIntegrationVersion);

            reconciliation = codeWorkflowContainerFacade.create(
                integrationDefinition.getComponentName(), integrationDefinition.getVersion(), workflowDefinitions,
                language, bytes, PlatformType.EMBEDDED, reusableWorkflowNameIds);

            integrationCodeWorkflowService.create(reconciliation.codeWorkflowContainer(), integration);
        }

        for (String workflowId : reconciliation.addedWorkflowNameIds()
            .values()) {

            integrationWorkflowService.addWorkflow(integrationId, draftIntegrationVersion, workflowId);
        }

        for (String workflowId : reconciliation.removedWorkflowNameIds()
            .values()) {

            integrationWorkflowService.delete(integrationId, draftIntegrationVersion, workflowId);

            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(workflowId);
            }

            workflowService.delete(workflowId);
        }
    }

    /**
     * Maps the published container's workflow names onto the current draft version's workflow ids. The facade publish
     * duplicates each workflow into the new draft version under a new workflow id but preserves the IntegrationWorkflow
     * uuid across both rows, so the chain is: published container name -> published workflow id -> row uuid at the
     * container's (published) version -> draft-version row with the same uuid -> draft workflow id.
     */
    private Map<String, String> resolveDraftWorkflowNameIds(
        IntegrationCodeWorkflow publishedIntegrationCodeWorkflow, long integrationId, int draftIntegrationVersion) {

        CodeWorkflowContainer publishedCodeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            publishedIntegrationCodeWorkflow.getCodeWorkflowContainerId());

        Map<String, String> publishedWorkflowNameIds = publishedCodeWorkflowContainer.getWorkflowNameIds();

        Map<String, String> workflowIdUuids = integrationWorkflowService.getIntegrationWorkflows(
            integrationId, publishedIntegrationCodeWorkflow.getIntegrationVersion())
            .stream()
            .collect(Collectors.toMap(IntegrationWorkflow::getWorkflowId, IntegrationWorkflow::getUuidAsString));

        Map<String, String> uuidDraftWorkflowIds = integrationWorkflowService.getIntegrationWorkflows(
            integrationId, draftIntegrationVersion)
            .stream()
            .collect(Collectors.toMap(IntegrationWorkflow::getUuidAsString, IntegrationWorkflow::getWorkflowId));

        Map<String, String> draftWorkflowNameIds = new HashMap<>();

        for (Map.Entry<String, String> entry : publishedWorkflowNameIds.entrySet()) {
            String uuid = workflowIdUuids.get(entry.getValue());

            String draftWorkflowId = uuid == null ? null : uuidDraftWorkflowIds.get(uuid);

            if (draftWorkflowId != null) {
                draftWorkflowNameIds.put(entry.getKey(), draftWorkflowId);
            }
        }

        return draftWorkflowNameIds;
    }

    private CodeWorkflowContainer getCodeWorkflowContainer(long integrationId) {
        IntegrationCodeWorkflow integrationCodeWorkflow = integrationCodeWorkflowService
            .fetchIntegrationCodeWorkflow(integrationId)
            .orElseThrow(() -> new ConfigurationException(
                "No code workflow exists for integration " + integrationId,
                CodeWorkflowErrorType.SOURCE_LOAD_FAILED));

        return codeWorkflowContainerService.getCodeWorkflowContainer(
            integrationCodeWorkflow.getCodeWorkflowContainerId());
    }

    private Optional<CodeWorkflowContainer> fetchCodeWorkflowContainer(
        IntegrationCodeWorkflow integrationCodeWorkflow) {

        try {
            return Optional.of(
                codeWorkflowContainerService.getCodeWorkflowContainer(
                    integrationCodeWorkflow.getCodeWorkflowContainerId()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void deployInto(
        Integration integration, IntegrationDefinition integrationDefinition, byte[] bytes, Language language) {

        List<WorkflowDefinition> workflowDefinitions = integrationDefinition.getWorkflows()
            .orElseGet(List::of);

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            integrationDefinition.getComponentName(), integrationDefinition.getVersion(), workflowDefinitions, language,
            bytes, PlatformType.EMBEDDED);

        integrationCodeWorkflowService.create(codeWorkflowContainer, integration);

        Map<String, String> workflowNameIds = codeWorkflowContainer.getWorkflowNameIds();

        for (Map.Entry<String, String> entry : workflowNameIds.entrySet()) {
            integrationWorkflowService.addWorkflow(
                integration.getId(), integration.getLastIntegrationVersion(), entry.getValue());
        }

        integrationService.publishIntegration(integration.getId(), null);
    }

    /**
     * Picks which integration an uploaded artifact redeploys into. The artifact carries only a component name, and a
     * component can now back several integrations, so a second code-backed one on the same component makes the upload
     * genuinely ambiguous — it fails rather than guessing. Visual integrations on the component are ignored: a code
     * artifact is not a redeploy of one.
     */
    private Integration resolveUploadTarget(IntegrationDefinition integrationDefinition) {
        String componentName = integrationDefinition.getComponentName();

        List<Integration> integrations = integrationService.getIntegrations(componentName)
            .stream()
            .filter(integration -> integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(integration.getId())
                .isPresent())
            .toList();

        if (integrations.size() > 1) {
            throw new ConfigurationException(
                "Component '" + componentName + "' backs more than one code integration, so an upload cannot tell "
                    + "which to redeploy; edit the one you mean in the source editor instead",
                CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS);
        }

        if (integrations.isEmpty()) {
            return createIntegration(integrationDefinition, null, null, null, null, null);
        }

        return updateIntegration(integrations.getFirst(), integrationDefinition);
    }

    /**
     * The caller's metadata wins over the starter template's: a description typed at creation is what the user meant,
     * while the template's is boilerplate. A blank name falls back to the component name, which is what the integration
     * was called before it could be named at all.
     */
    private Integration createIntegration(
        IntegrationDefinition integrationDefinition, @Nullable String name, @Nullable String description,
        @Nullable Long categoryId, @Nullable List<String> tags, @Nullable String permissionExpression) {

        Integration integration = new Integration();

        integration.setComponentName(integrationDefinition.getComponentName());
        integration.setComponentVersion(integrationDefinition.getComponentVersion());
        integration.setDescription(
            description == null || description.isBlank()
                ? integrationDefinition.getDescription()
                    .orElse(null)
                : description);
        integration.setName(
            name == null || name.isBlank() ? integrationDefinition.getComponentName() : name);
        integration.setPermissionExpression(permissionExpression);

        if (categoryId != null) {
            integration.setCategory(new Category(categoryId));
        }

        if (tags != null && !tags.isEmpty()) {
            integration.setTags(
                tagService.save(
                    tags.stream()
                        .map(Tag::new)
                        .toList()));
        }

        return integrationService.create(integration);
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - Temporary files are created with system-generated names in the temp directory,
     * not user-controlled paths. Access is restricted to administrators.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private IntegrationDefinition loadIntegrationDefinition(Language language, byte[] bytes) throws IOException {
        Path path = Files.createTempFile("code_workflow_integration", language.getExtension());

        Files.write(path, bytes);

        URI uri = path.toUri();

        try {
            IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
                uri.toURL(), language, javaLoader, uri + UUID.randomUUID()
                    .toString(),
                cacheManager);

            return integrationHandler.getDefinition();
        } finally {
            Files.delete(path);
        }
    }

    private Integration updateIntegration(Integration integration, IntegrationDefinition integrationDefinition) {
        integration.setComponentVersion(integrationDefinition.getComponentVersion());
        integration.setDescription(
            integrationDefinition.getDescription()
                .orElse(null));

        return integrationService.update(integration);
    }

    private static String readTemplate(Language language) {
        String resource = "integration-code-workflow-templates/starter." + language.getExtension();

        try (InputStream inputStream = IntegrationCodeWorkflowFacadeImpl.class.getClassLoader()
            .getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
