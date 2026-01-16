/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionModel;
import com.bytechef.exception.ConfigurationException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/api-platform/internal")
@ConditionalOnCoordinator
public class ApiCollectionApiController implements ApiCollectionApi {

    private final ApiCollectionFacade apiCollectionFacade;
    private final ApiCollectionService apiCollectionService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiCollectionApiController(
        ApiCollectionFacade apiCollectionFacade, ApiCollectionService apiCollectionService,
        ConversionService conversionService) {

        this.apiCollectionFacade = apiCollectionFacade;
        this.apiCollectionService = apiCollectionService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ApiCollectionModel> createApiCollection(ApiCollectionModel apiCollectionModel) {
        validate(apiCollectionModel);

        ApiCollectionDTO apiCollection = conversionService.convert(apiCollectionModel, ApiCollectionDTO.class);

        return ResponseEntity.ok(
            conversionService.convert(
                apiCollectionFacade.createApiCollection(apiCollection), ApiCollectionModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteApiCollection(Long id) {
        apiCollectionFacade.deleteApiCollection(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ApiCollectionModel> getApiCollection(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(apiCollectionFacade.getApiCollection(id), ApiCollectionModel.class));
    }

    @GetMapping("/api-collections/{id}/openapi.json")
    @ResponseBody
    public ResponseEntity<Resource> getOpenApiSpecification(@PathVariable("id") long id) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();

        bodyBuilder.contentType(MediaType.APPLICATION_OCTET_STREAM);

        bodyBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + getFilename(id) + "\"");

        String openApiSpecification = apiCollectionFacade.getOpenApiSpecification(id);

        return bodyBuilder.body(new ByteArrayResource(openApiSpecification.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public ResponseEntity<List<ApiCollectionModel>> getWorkspaceApiCollections(
        Long id, Long environmentId, Long projectId, Long tagId) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                apiCollectionFacade.getApiCollections(id, environmentId, projectId, tagId),
                openApiCollection -> conversionService.convert(openApiCollection, ApiCollectionModel.class)));
    }

    @Override
    public ResponseEntity<ApiCollectionModel> updateApiCollection(Long id, ApiCollectionModel apiCollectionModel) {
        validate(apiCollectionModel);

        return ResponseEntity.ok(
            conversionService.convert(
                apiCollectionFacade.updateApiCollection(
                    conversionService.convert(apiCollectionModel.id(id), ApiCollectionDTO.class)),
                ApiCollectionModel.class));
    }

    private String getFilename(long id) {
        ApiCollection apiCollection = apiCollectionService.getApiCollection(id);

        String name = apiCollection.getName();

        name = name.toLowerCase()
            .replace(" ", "_")
            .trim();

        return "%s_openapi.json".formatted(name);
    }

    private static void validate(ApiCollectionModel apiCollectionModel) {
        String contextPath = apiCollectionModel.getContextPath();

        if (contextPath.startsWith("/")) {
            throw new ConfigurationException(
                "Context path must not start with a slash.", ApiCollectionErrorType.INVALID_CONTEXT_PATH);
        }
    }
}
