/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static com.bytechef.file.storage.base64.service.Base64FileStorageService.URL_PREFIX;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.ee.embedded.execution.facade.ActionFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExecuteActionRequestModel;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ActionApiController implements ActionApi {

    private static final String BODY_CONTENT = "bodyContent";
    private static final String BODY_CONTENT_TYPE = "bodyContentType";
    private static final String FILENAME = "filename";
    private static final String VALUE = "value";

    private final ActionFacade actionFacade;
    private final EnvironmentService environmentService;

    public ActionApiController(ActionFacade actionFacade, EnvironmentService environmentService) {
        this.actionFacade = actionFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<Object> executeAction(
        String externalUserId, String componentName, Integer componentVersion, String actionName,
        ExecuteActionRequestModel executeActionRequestModel, EnvironmentModel xEnvironment, Long xInstanceId) {

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        Map<String, Object> inputParameters = new HashMap<>(executeActionRequestModel.getInput());

        if (MapUtils.containsKey(inputParameters, BODY_CONTENT_TYPE)) {
            Http.BodyContentType bodyContentType = MapUtils.get(
                inputParameters, BODY_CONTENT_TYPE, Http.BodyContentType.class);

            if (bodyContentType == Http.BodyContentType.BINARY) {
                inputParameters.put(
                    BODY_CONTENT,
                    new FileEntryImpl(
                        new FileEntry(
                            MapUtils.getRequiredString(inputParameters, FILENAME),
                            URL_PREFIX + MapUtils.getRequiredString(inputParameters, BODY_CONTENT))));
            } else if (bodyContentType == Http.BodyContentType.FORM_DATA) {
                Map<String, Object> bodyContent = new HashMap<>(
                    MapUtils.getMap(inputParameters, BODY_CONTENT, Map.of()));

                for (Map.Entry<String, Object> entry : bodyContent.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> entryValue = (Map<String, ?>) entry.getValue();

                    if (Objects.equals(entryValue.get("type"), "FILE")) {
                        String filename = MapUtils.getRequiredString(entryValue, FILENAME);
                        String value = MapUtils.getRequiredString(entryValue, VALUE);

                        bodyContent.put(entry.getKey(), new FileEntryImpl(new FileEntry(filename, URL_PREFIX + value)));
                    } else {
                        bodyContent.put(entry.getKey(), entryValue.get(VALUE));
                    }
                }

                inputParameters.put(BODY_CONTENT, bodyContent);
            }
        }

        return ResponseEntity.ok(
            actionFacade.executeAction(
                externalUserId, componentName, componentVersion, actionName, inputParameters, xInstanceId,
                environment));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private static class FileEntryImpl implements com.bytechef.component.definition.FileEntry {

        @Nullable
        private String extension;
        @Nullable
        private String mimeType;
        private String name;
        private String url;

        private FileEntryImpl() {
        }

        public FileEntryImpl(FileEntry fileEntry) {
            this(fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl());
        }

        public FileEntryImpl(String name, @Nullable String extension, @Nullable String mimeType, String url) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.name = Objects.requireNonNull(name);
            this.url = Objects.requireNonNull(url);
        }

        @Override
        @Nullable
        public String getExtension() {
            return extension;
        }

        @Override
        @Nullable
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "FileEntryImpl{" +
                "extension='" + extension + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
        }
    }
}
