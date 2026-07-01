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

package com.bytechef.component.approval.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalRequestApprovalActionTest {

    @Test
    void testPerformRoutesApprovalChannelThroughTokenRefreshAwareService() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        ComponentConnection componentConnection = new ComponentConnection(
            "googleMail", 1, 5L, Map.of("access_token", "expired"), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "approvalChannels",
                    List.of(
                        Map.of(
                            "name", "googleMail_1",
                            "type", "googleMail/v1/googleMail",
                            "parameters", Map.of("subject", "Please approve"))))));

        performFunction.apply(inputParameters, Map.of("googleMail_1", componentConnection), extensions, context);

        // The channel must run through the aspect-wrapped service method, with the form URL derived from the resume URL
        // and the channel's own connection threaded in so a token refresh can retry against fresh credentials.
        verify(clusterElementDefinitionService, times(1)).executeApprovalChannel(
            eq("googleMail"), eq(1), eq("googleMail"), any(), eq("https://example.com/api/resume/abc"),
            eq(componentConnection), eq(context));
    }
}
