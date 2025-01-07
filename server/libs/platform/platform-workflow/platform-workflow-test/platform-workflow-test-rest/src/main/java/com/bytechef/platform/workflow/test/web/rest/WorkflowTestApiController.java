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

package com.bytechef.platform.workflow.test.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.workflow.test.facade.WorkflowTestFacade;
import com.bytechef.platform.workflow.test.web.rest.model.TestWorkflowRequestModel;
import com.bytechef.platform.workflow.test.web.rest.model.WorkflowTestExecutionModel;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class WorkflowTestApiController implements WorkflowTestApi {

    private final FilesFileStorage filesFileStorage;

    private static final Pattern IMAGE_PATTERN = Pattern.compile("data:image/[^;]+;base64,([^\\s]+)");
    private static final Pattern TEXT_PATTERN = Pattern.compile(
        "<attachment[^>]*>(.*?)</attachment>", Pattern.DOTALL);

    private final ConversionService conversionService;
    private final WorkflowTestFacade workflowTestFacade;

    public WorkflowTestApiController(
        FilesFileStorage filesFileStorage, ConversionService conversionService, WorkflowTestFacade workflowTestFacade) {

        this.filesFileStorage = filesFileStorage;
        this.conversionService = conversionService;
        this.workflowTestFacade = workflowTestFacade;
    }

    @Override
    public ResponseEntity<WorkflowTestExecutionModel> testWorkflow(
        String id, TestWorkflowRequestModel testWorkflowRequestModel) {

        Map<String, Object> inputs = testWorkflowRequestModel == null ? Map.of() : testWorkflowRequestModel.getInputs();

        if (inputs.containsKey("trigger_1")) {
            Map<String, Object> trigger1 = MapUtils.getRequiredMap(inputs, "trigger_1", new TypeReference<>() {});

            if (trigger1.containsKey("attachments")) {
                trigger1.put("attachments", getFileEntries(trigger1));
            } else {
                trigger1.put("attachments", List.of());
            }

            inputs.put("trigger_1", trigger1);
        }

        return ResponseEntity.ok(
            conversionService.convert(workflowTestFacade.testWorkflow(id, inputs), WorkflowTestExecutionModel.class));
    }

    private List<FileEntry> getFileEntries(Map<String, Object> trigger1) {
        List<Map<String, Object>> attachments = MapUtils.getRequiredList(
            trigger1, "attachments", new TypeReference<>() {});

        List<FileEntry> fileEntries = new ArrayList<>();

        for (Map<String, Object> attachment : attachments) {
            List<Map<String, String>> content = MapUtils.getRequiredList(
                attachment, "content", new TypeReference<>() {});
            String contentType = MapUtils.getString(attachment, "contentType");
            String name = (String) attachment.get("name");

            if (contentType.startsWith("text/")) {
                Matcher matcher = TEXT_PATTERN.matcher(MapUtils.getString(content.getFirst(), "text"));

                if (matcher.find()) {
                    String text = matcher.group(1);

                    fileEntries.add(filesFileStorage.storeFileContent(name, text));
                }
            } else {
                Matcher matcher = IMAGE_PATTERN.matcher(MapUtils.getString(content.getFirst(), "image"));

                if (matcher.find()) {
                    fileEntries.add(filesFileStorage.storeFileContent(
                        name, new ByteArrayInputStream(EncodingUtils.base64Decode(matcher.group(1)))));
                }
            }
        }

        return fileEntries;
    }
}
