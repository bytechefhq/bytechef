
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.athena.configuration.web.rest;

import com.bytechef.athena.configuration.constant.IntegrationConstants;
import com.bytechef.athena.configuration.facade.IntegrationFacade;
import com.bytechef.athena.configuration.web.rest.model.WorkflowModel;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.athena.configuration.web.rest.workflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class WorkflowApiController implements WorkflowApi {

    private final IntegrationFacade integrationFacade;
    private final ConversionService conversionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        IntegrationFacade integrationFacade, ConversionService conversionService, WorkflowService workflowService) {
        this.integrationFacade = integrationFacade;

        this.conversionService = conversionService;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Void> deleteIntegrationWorkflow(Long id, String workflowId) {
        integrationFacade.deleteWorkflow(id, workflowId);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getIntegrationWorkflows(Long id) {
        return ResponseEntity.ok(
            integrationFacade.getIntegrationWorkflows(id)
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return ResponseEntity.ok(
            Validate.notNull(
                conversionService.convert(workflowService.getWorkflow(id), WorkflowModel.class), "workflowModel")
                .definition(null));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (Workflow workflow : workflowService.getWorkflows(IntegrationConstants.INTEGRATION_TYPE)) {
            workflowModels.add(
                Validate.notNull(conversionService.convert(workflow, WorkflowModel.class), "workflowModel")
                    .definition(null));
        }

        return ResponseEntity.ok(workflowModels);
    }

    @Override
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(workflowService.update(
                id, workflowModel.getDefinition()), WorkflowModel.class));
    }
}
