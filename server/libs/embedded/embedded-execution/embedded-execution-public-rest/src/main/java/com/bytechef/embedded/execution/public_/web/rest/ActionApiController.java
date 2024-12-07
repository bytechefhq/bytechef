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

package com.bytechef.embedded.execution.public_.web.rest;

import static com.bytechef.file.storage.base64.service.Base64FileStorageService.URL_PREFIX;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.embedded.connectivity.facade.ActionFacade;
import com.bytechef.embedded.execution.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.execution.public_.web.rest.model.ExecuteAction200ResponseModel;
import com.bytechef.embedded.execution.public_.web.rest.model.ExecuteActionRequestModel;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.constant.Environment;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class ActionApiController implements ActionApi {

    private static final String BODY_CONTENT = "bodyContent";
    private static final String BODY_CONTENT_TYPE = "bodyContentType";
    private static final String FILENAME = "filename";
    private static final String VALUE = "value";

    private final ActionFacade actionFacade;

    public ActionApiController(ActionFacade actionFacade) {
        this.actionFacade = actionFacade;
    }

    @Override
    public ResponseEntity<ExecuteAction200ResponseModel> executeAction(
        String componentName, Integer componentVersion, String actionName, EnvironmentModel xEnvironment,
        Long xInstanceId, ExecuteActionRequestModel executeActionRequestModel) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

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

                        bodyContent.put(
                            entry.getKey(),
                            new FileEntryImpl(new FileEntry(filename, URL_PREFIX + value)));
                    } else {
                        bodyContent.put(entry.getKey(), entryValue.get(VALUE));
                    }
                }

                inputParameters.put(BODY_CONTENT, bodyContent);
            }
        }

        return ResponseEntity.ok(
            new ExecuteAction200ResponseModel().output(
                actionFacade.executeAction(
                    componentName, componentVersion, actionName, inputParameters, environment, xInstanceId)));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private static class FileEntryImpl implements com.bytechef.component.definition.FileEntry {

        private String extension;
        private String mimeType;
        private String name;
        private String url;

        private FileEntryImpl() {
        }

        public FileEntryImpl(FileEntry fileEntry) {
            this(fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getName(), fileEntry.getUrl());
        }

        public FileEntryImpl(String extension, String mimeType, String name, String url) {
            this.extension = Objects.requireNonNull(extension);
            this.mimeType = Objects.requireNonNull(mimeType);
            this.name = Objects.requireNonNull(name);
            this.url = Objects.requireNonNull(url);
        }

        @Override

        public String getExtension() {
            return extension;
        }

        @Override
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
