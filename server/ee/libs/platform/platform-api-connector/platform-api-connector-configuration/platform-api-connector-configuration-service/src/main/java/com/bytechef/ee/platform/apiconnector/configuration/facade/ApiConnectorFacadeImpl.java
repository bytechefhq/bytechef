/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorEndpoint;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorEndpoint.HttpMethod;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import com.bytechef.ee.platform.apiconnector.configuration.exception.ApiConnectorErrorType;
import com.bytechef.ee.platform.apiconnector.configuration.generator.OpenApiComponentDefinitionFactory;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorAiService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
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
public class ApiConnectorFacadeImpl implements ApiConnectorFacade {

    private static final Logger log = LoggerFactory.getLogger(ApiConnectorFacadeImpl.class);

    private final ApiConnectorAiService apiConnectorAiService;
    private final ApiConnectorFileStorage apiConnectorFileStorage;
    private final ApiConnectorService apiConnectorService;

    @SuppressFBWarnings("EI")
    public ApiConnectorFacadeImpl(
        @Nullable ApiConnectorAiService apiConnectorAiService,
        ApiConnectorFileStorage apiConnectorFileStorage,
        ApiConnectorService apiConnectorService) {

        this.apiConnectorAiService = apiConnectorAiService;
        this.apiConnectorFileStorage = apiConnectorFileStorage;
        this.apiConnectorService = apiConnectorService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteApiConnector(long id) {
        ApiConnector apiConnector = apiConnectorService.getApiConnector(id);

        apiConnectorService.delete(id);

        deleteFileEntry(apiConnector.getDefinition(), apiConnectorFileStorage::deleteApiConnectorDefinition);
        deleteFileEntry(apiConnector.getSpecification(), apiConnectorFileStorage::deleteApiConnectorSpecification);
    }

    /**
     * Importing/generating a connector registers a new component with the platform runtime, so it is restricted to
     * administrators. The guard lives on the facade so it protects every caller, not only the GraphQL entry point.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ApiConnector generateFromDocumentation(String name, String documentationUrl, String icon) {
        if (apiConnectorAiService == null) {
            throw new ConfigurationException(
                "AI service is not configured. Please configure bytechef.ai.copilot settings.",
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        String specification = apiConnectorAiService.generateOpenApiSpecification(documentationUrl);

        return importOpenApiSpecification(name, icon, specification);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ApiConnector importOpenApiSpecification(String name, String icon, String specification) {
        name = convertComponentName(name);

        GeneratedDefinition generatedDefinition = generateDefinition(name, specification);

        ApiConnector apiConnector = apiConnectorService.fetchApiConnector(name, 1)
            .orElse(null);
        boolean isNew = false;

        if (apiConnector == null) {
            apiConnector = new ApiConnector();
            isNew = true;
        }

        FileEntry oldDefinition = apiConnector.getDefinition();
        FileEntry oldSpecification = apiConnector.getSpecification();

        applyGeneratedDefinition(apiConnector, name, icon, specification, generatedDefinition);

        if (isNew) {
            return apiConnectorService.create(apiConnector);
        }

        ApiConnector updatedApiConnector = apiConnectorService.update(apiConnector);

        deleteReplacedFileEntry(oldDefinition, updatedApiConnector.getDefinition(),
            apiConnectorFileStorage::deleteApiConnectorDefinition);
        deleteReplacedFileEntry(oldSpecification, updatedApiConnector.getSpecification(),
            apiConnectorFileStorage::deleteApiConnectorSpecification);

        return updatedApiConnector;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ApiConnector updateApiConnector(long id, String name, String icon, String specification, Integer version) {
        ApiConnector curApiConnector = apiConnectorService.getApiConnector(id);

        if (version != null && curApiConnector.getVersion() != version) {
            throw new ConfigurationException(
                "The API connector was modified by someone else; reload it and reapply the changes",
                ApiConnectorErrorType.API_CONNECTOR_VERSION_CONFLICT);
        }

        String componentName = name == null ? curApiConnector.getName() : convertComponentName(name);

        apiConnectorService.fetchApiConnector(componentName, 1)
            .filter(existingApiConnector -> !Objects.equals(existingApiConnector.getId(), id))
            .ifPresent(existingApiConnector -> {
                throw new ConfigurationException(
                    "An API connector named '%s' already exists".formatted(componentName),
                    ApiConnectorErrorType.API_CONNECTOR_NAME_ALREADY_EXISTS);
            });

        GeneratedDefinition generatedDefinition = generateDefinition(componentName, specification);

        FileEntry oldDefinition = curApiConnector.getDefinition();
        FileEntry oldSpecification = curApiConnector.getSpecification();

        applyGeneratedDefinition(curApiConnector, componentName, icon, specification, generatedDefinition);

        ApiConnector updatedApiConnector = apiConnectorService.update(curApiConnector);

        deleteReplacedFileEntry(oldDefinition, updatedApiConnector.getDefinition(),
            apiConnectorFileStorage::deleteApiConnectorDefinition);
        deleteReplacedFileEntry(oldSpecification, updatedApiConnector.getSpecification(),
            apiConnectorFileStorage::deleteApiConnectorSpecification);

        return updatedApiConnector;
    }

    private GeneratedDefinition generateDefinition(String name, String specification) {
        Path openApiSpecificationPath;

        try {
            openApiSpecificationPath = Files.createTempFile("open_api_specification", ".yaml");
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        try {
            Files.writeString(openApiSpecificationPath, specification);
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        OpenAPI openAPI = parseOpenAPIFile(openApiSpecificationPath.toString());

        try {
            return new GeneratedDefinition(
                openAPI, OpenApiComponentDefinitionFactory.createComponentDefinitionJson(name, openAPI));
        } catch (RuntimeException e) {
            throw new ConfigurationException(e.getMessage(), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }
    }

    private void applyGeneratedDefinition(
        ApiConnector apiConnector, String name, String icon, String specification,
        GeneratedDefinition generatedDefinition) {

        OpenAPI openAPI = generatedDefinition.openAPI();

        apiConnector.setName(name);
        apiConnector.setConnectorVersion(1);
        apiConnector.setDefinition(
            apiConnectorFileStorage.storeApiConnectorDefinition("definition.json", generatedDefinition.definition()));
        apiConnector.setDescription(getDescription(openAPI.getInfo()));
        apiConnector.setSpecification(
            apiConnectorFileStorage.storeApiConnectorSpecification("specification.yaml", specification));
        apiConnector.setTitle(getTitle(name, openAPI.getInfo()));

        if (icon != null) {
            apiConnector.setIcon(icon);
        }
    }

    private record GeneratedDefinition(OpenAPI openAPI, String definition) {
    }

    /**
     * Every store writes a new physical blob even under an identical logical filename, so a successful update leaves
     * the previously referenced blob orphaned. Deletion is best-effort: a storage failure must not undo an update that
     * already committed.
     */
    private void deleteReplacedFileEntry(FileEntry oldFileEntry, FileEntry newFileEntry, Consumer<FileEntry> delete) {
        if (oldFileEntry == null || Objects.equals(oldFileEntry.getUrl(), newFileEntry.getUrl())) {
            return;
        }

        deleteFileEntry(oldFileEntry, delete);
    }

    private void deleteFileEntry(FileEntry fileEntry, Consumer<FileEntry> delete) {
        if (fileEntry == null) {
            return;
        }

        try {
            delete.accept(fileEntry);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete API connector file {}", fileEntry.getName(), exception);
        }
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public int deleteOrphanedFiles() {
        Set<String> referencedUrls = new HashSet<>();

        for (ApiConnector apiConnector : apiConnectorService.getApiConnectors()) {
            if (apiConnector.getDefinition() != null) {
                referencedUrls.add(apiConnector.getDefinition()
                    .getUrl());
            }

            if (apiConnector.getSpecification() != null) {
                referencedUrls.add(apiConnector.getSpecification()
                    .getUrl());
            }
        }

        int deletedFiles = 0;

        for (FileEntry fileEntry : apiConnectorFileStorage.getApiConnectorDefinitionFileEntries()) {
            if (!referencedUrls.contains(fileEntry.getUrl())) {
                deleteFileEntry(fileEntry, apiConnectorFileStorage::deleteApiConnectorDefinition);

                deletedFiles++;
            }
        }

        for (FileEntry fileEntry : apiConnectorFileStorage.getApiConnectorSpecificationFileEntries()) {
            if (!referencedUrls.contains(fileEntry.getUrl())) {
                deleteFileEntry(fileEntry, apiConnectorFileStorage::deleteApiConnectorSpecification);

                deletedFiles++;
            }
        }

        return deletedFiles;
    }

    @Override
    public ApiConnectorDTO getApiConnector(long id) {
        return toApiConnectorDTO(apiConnectorService.getApiConnector(id));
    }

    @Override
    public List<ApiConnectorDTO> getApiConnectors() {
        return apiConnectorService.getApiConnectors()
            .stream()
            .map(this::toApiConnectorDTO)
            .toList();
    }

    private ApiConnectorDTO toApiConnectorDTO(ApiConnector apiConnector) {
        String specification = apiConnectorFileStorage.readApiConnectorSpecification(apiConnector.getSpecification());

        return new ApiConnectorDTO(
            apiConnector, apiConnectorFileStorage.readApiConnectorDefinition(apiConnector.getDefinition()),
            specification, getEndpoints(apiConnector, specification));
    }

    /**
     * Endpoint extraction is fail-soft: a connector whose stored specification no longer parses (e.g. hand-edited) must
     * degrade to an empty endpoint list instead of failing the whole connector list query.
     */
    private List<ApiConnectorEndpoint> getEndpoints(ApiConnector apiConnector, String specification) {
        OpenAPI openAPI;

        try {
            openAPI = parseOpenAPIContent(specification);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to parse the stored specification of API connector {}", apiConnector.getName(), exception);

            return List.of();
        }

        Paths paths = openAPI.getPaths();

        if (paths == null) {
            return List.of();
        }

        return paths
            .entrySet()
            .stream()
            .flatMap(entry -> {
                List<ApiConnectorEndpoint> curEndpoints = new ArrayList<>();

                String path = entry.getKey();
                PathItem pathItem = entry.getValue();

                if (pathItem.getDelete() != null) {
                    Operation operation = pathItem.getDelete();

                    ApiConnectorEndpoint endpoint = new ApiConnectorEndpoint(
                        path, operation.getOperationId(), operation.getDescription(), HttpMethod.DELETE);

                    endpoint.setId(generateEndpointId(path, HttpMethod.DELETE));

                    curEndpoints.add(endpoint);
                }

                if (pathItem.getGet() != null) {
                    Operation operation = pathItem.getGet();

                    ApiConnectorEndpoint endpoint = new ApiConnectorEndpoint(
                        path, operation.getOperationId(), operation.getDescription(), HttpMethod.GET);

                    endpoint.setId(generateEndpointId(path, HttpMethod.GET));

                    curEndpoints.add(endpoint);
                }

                if (pathItem.getHead() != null) {
                    Operation operation = pathItem.getHead();

                    ApiConnectorEndpoint endpoint = new ApiConnectorEndpoint(
                        path, operation.getOperationId(), operation.getDescription(), HttpMethod.HEAD);

                    endpoint.setId(generateEndpointId(path, HttpMethod.HEAD));

                    curEndpoints.add(endpoint);
                }

                if (pathItem.getPatch() != null) {
                    Operation operation = pathItem.getPatch();

                    ApiConnectorEndpoint endpoint = new ApiConnectorEndpoint(
                        path, operation.getOperationId(), operation.getDescription(), HttpMethod.PATCH);

                    endpoint.setId(generateEndpointId(path, HttpMethod.PATCH));

                    curEndpoints.add(endpoint);
                }

                if (pathItem.getPost() != null) {
                    Operation operation = pathItem.getPost();

                    ApiConnectorEndpoint endpoint = new ApiConnectorEndpoint(
                        path, operation.getOperationId(), operation.getDescription(), HttpMethod.POST);

                    endpoint.setId(generateEndpointId(path, HttpMethod.POST));

                    curEndpoints.add(endpoint);
                }

                if (pathItem.getPut() != null) {
                    Operation operation = pathItem.getPut();

                    ApiConnectorEndpoint endpoint = new ApiConnectorEndpoint(
                        path, operation.getOperationId(), operation.getDescription(), HttpMethod.PUT);

                    endpoint.setId(generateEndpointId(path, HttpMethod.PUT));

                    curEndpoints.add(endpoint);
                }

                return CollectionUtils.stream(curEndpoints);
            })
            .toList();
    }

    private static long generateEndpointId(String path, HttpMethod httpMethod) {
        String combined = path + ":" + httpMethod.name();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Use first 8 bytes of SHA-256 hash for a long value with very low collision probability
            long result = 0;

            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hashBytes[i] & 0xFF);
            }

            return Math.abs(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("SHA-256 algorithm not available", exception);
        }
    }

    // Package-private for unit testing of the path-traversal validation.
    static String convertComponentName(String componentName) {
        componentName = StringUtils.trim(componentName);

        componentName = componentName.toLowerCase();

        componentName = StringUtils.replaceChars(componentName, "-_", ".");

        componentName = StringUtils.replaceChars(componentName, " ", "");

        validateComponentName(componentName);

        return componentName;
    }

    /**
     * The component name is used to build a package/directory path for the generated component sources, so it must not
     * be able to escape that location. Reject anything outside a simple {@code [a-z0-9.]} identifier (which excludes
     * path separators) and any dotted form that could traverse upward.
     */
    private static void validateComponentName(String componentName) {
        if (StringUtils.isBlank(componentName) || !componentName.matches("[a-z0-9.]+") ||
            componentName.contains("..") || StringUtils.startsWith(componentName, ".") ||
            StringUtils.endsWith(componentName, ".")) {

            throw new ConfigurationException(
                "Invalid API connector name; only letters, digits, and dots are allowed",
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }
    }

    private String getDescription(Info info) {
        return info.getDescription();
    }

    private String getTitle(String componentName, Info info) {
        String title = info.getTitle();

        if (StringUtils.isEmpty(title)) {
            String[] items = componentName.split("-");

            title = Arrays.stream(items)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(""));
        }

        return title;
    }

    private OpenAPI parseOpenAPIFile(String openApiPath) {
        SwaggerParseResult result = new OpenAPIParser().readLocation(openApiPath, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        List<String> messages = result.getMessages();

        if (messages != null && !messages.isEmpty()) {
            throw new ConfigurationException(
                String.join("\n", messages), ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return openAPI;
    }

    /**
     * Unlike {@link #parseOpenAPIFile}, which validates strictly because its result feeds code generation, this parse
     * only backs read models, so parser warnings are tolerated as long as a document came back at all.
     */
    private OpenAPI parseOpenAPIContent(String specification) {
        SwaggerParseResult result = new OpenAPIParser().readContents(specification, null, null);

        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            List<String> messages = result.getMessages();

            throw new IllegalArgumentException(
                messages == null || messages.isEmpty() ? "Invalid OpenAPI specification"
                    : String.join("\n", messages));
        }

        return openAPI;
    }
}
