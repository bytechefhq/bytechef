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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class PreBuiltTemplateService {

    private final GitHubProxyClient gitHubProxyClient;
    private final TemplatesProperties templatesProperties;

    @SuppressFBWarnings("EI")
    public PreBuiltTemplateService(
        GitHubProxyClient gitHubProxyClient, TemplatesProperties templatesProperties) {

        this.gitHubProxyClient = gitHubProxyClient;
        this.templatesProperties = templatesProperties;
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
}
