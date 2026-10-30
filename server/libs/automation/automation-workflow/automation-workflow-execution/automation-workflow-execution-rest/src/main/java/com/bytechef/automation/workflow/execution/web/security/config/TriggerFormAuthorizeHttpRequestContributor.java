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

package com.bytechef.automation.workflow.execution.web.security.config;

import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Permits anonymous access to the public trigger form served by
 * {@link com.bytechef.automation.workflow.execution.web.rest.TriggerFormApiController}. The unguessable random-v4
 * workflow uuid in the WorkflowExecutionId is the capability, like a webhook URL.
 *
 * @author Ivica Cardic
 */
@Component
public class TriggerFormAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    @Override
    public List<String> getApiPermitAllRequestMatcherPaths() {
        return List.of("/api/automation/internal/trigger-form/**");
    }
}
