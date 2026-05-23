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

package com.bytechef.platform.githubproxy.client;

/**
 * Client for the bytechef-github-proxy's workflow-template endpoints ({@code /workflow-templates} and
 * {@code /workflow-templates/{slug}}).
 *
 * @author Ivica Cardic
 */
public interface WorkflowTemplateProxyClient {

    /**
     * Fetches one page of workflow-template summaries.
     */
    WorkflowTemplatePage getWorkflowTemplates(int page, int size);

    /**
     * Fetches one full workflow template by slug. Throws an {@code HttpClientErrorException.NotFound} when the proxy
     * responds 404.
     */
    WorkflowTemplate getWorkflowTemplate(String slug);
}
