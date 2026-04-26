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

package com.bytechef.platform.workflow.execution.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.workflow.execution.facade.ApprovalFormFacade;
import com.bytechef.platform.workflow.execution.web.rest.model.ApprovalFormModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.platform.workflow.execution.web.rest.ApprovalFormApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class ApprovalFormApiController implements ApprovalFormApi {

    private final ApprovalFormFacade approvalFormFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApprovalFormApiController(ApprovalFormFacade approvalFormFacade, ConversionService conversionService) {
        this.approvalFormFacade = approvalFormFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ApprovalFormModel> getApprovalForm(String id) {
        Map<String, ?> parameters;

        try {
            parameters = approvalFormFacade.getApprovalForm(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id: " + id, exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval form is no longer available.", exception);
        }

        return ResponseEntity.ok(conversionService.convert(parameters, ApprovalFormModel.class));
    }
}
