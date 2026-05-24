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

package com.bytechef.platform.githubproxy.client.internal;

import com.bytechef.platform.githubproxy.client.WorkflowTemplateProxyClient;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplate;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplatePage;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
public class RestWorkflowTemplateProxyClient implements WorkflowTemplateProxyClient {

    private final RestClient restClient;

    public RestWorkflowTemplateProxyClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    @Override
    public WorkflowTemplatePage getWorkflowTemplates(int page, int size) {
        WorkflowTemplatePage workflowTemplatePage = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/workflow-templates")
                .queryParam("page", page)
                .queryParam("size", size)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(WorkflowTemplatePage.class);

        if (workflowTemplatePage == null) {
            throw new IllegalStateException("Empty response from the workflow-template proxy");
        }

        return workflowTemplatePage;
    }

    @Override
    public WorkflowTemplate getWorkflowTemplate(String slug) {
        WorkflowTemplate workflowTemplate = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/workflow-templates/{slug}")
                .build(slug))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(WorkflowTemplate.class);

        if (workflowTemplate == null) {
            throw new IllegalStateException("Empty response from the workflow-template proxy for slug: " + slug);
        }

        return workflowTemplate;
    }
}
