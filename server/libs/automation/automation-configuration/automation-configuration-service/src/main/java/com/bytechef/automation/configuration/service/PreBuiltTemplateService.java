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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.config.TemplatesProperties;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.githubproxy.client.FileItem;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.githubproxy.client.WorkflowTemplate;
import com.bytechef.platform.githubproxy.client.WorkflowTemplatePage;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateProxyClient;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateSummary;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class PreBuiltTemplateService {

    private final GitHubProxyClient gitHubProxyClient;
    private final TemplatesProperties templatesProperties;
    private final WorkflowTemplateProxyClient workflowTemplateProxyClient;

    @SuppressFBWarnings("EI")
    public PreBuiltTemplateService(
        GitHubProxyClient gitHubProxyClient, TemplatesProperties templatesProperties,
        WorkflowTemplateProxyClient workflowTemplateProxyClient) {

        this.gitHubProxyClient = gitHubProxyClient;
        this.templatesProperties = templatesProperties;
        this.workflowTemplateProxyClient = workflowTemplateProxyClient;
    }

    public List<FileItem> getFiles(String directory) {
        return gitHubProxyClient.listFiles(
            templatesProperties.getOwner(), templatesProperties.getRepo(), templatesProperties.getRef(), directory);
    }

    @Cacheable("prebuilt-template")
    public byte[] getPrebuiltTemplateData(String id) {
        return gitHubProxyClient.getRaw(
            templatesProperties.getOwner(), templatesProperties.getRepo(),
            templatesProperties.getRef(), EncodingUtils.base64DecodeToString(id), null, null, null);
    }

    public List<WorkflowTemplateSummary> getWorkflowTemplates() {
        List<WorkflowTemplateSummary> summaries = new ArrayList<>();

        int page = 0;
        int totalPages = 1;

        while (page < totalPages) {
            WorkflowTemplatePage workflowTemplatePage = workflowTemplateProxyClient.getWorkflowTemplates(page, 200);

            summaries.addAll(workflowTemplatePage.content());

            totalPages = workflowTemplatePage.totalPages();
            page += 1;
        }

        return summaries;
    }

    public WorkflowTemplate getWorkflowTemplate(String slug) {
        return workflowTemplateProxyClient.getWorkflowTemplate(slug);
    }
}
