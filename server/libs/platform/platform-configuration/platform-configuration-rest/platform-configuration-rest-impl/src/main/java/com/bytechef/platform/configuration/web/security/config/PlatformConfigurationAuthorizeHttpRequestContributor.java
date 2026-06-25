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

package com.bytechef.platform.configuration.web.security.config;

import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class PlatformConfigurationAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    @Override
    public List<String> getApiPermitAllRequestMatcherPaths() {
        // The trigger-form endpoint is served by the automation TriggerFormApiController at
        // /api/automation/internal/trigger-form/{id}; it is a public form (the unguessable random-v4 workflow uuid in
        // the WorkflowExecutionId is the capability, like a webhook URL). The previous /api/platform/... path matched
        // no
        // controller, so anonymous form loads failed.
        return List.of("/api/automation/internal/trigger-form/**");
    }
}
