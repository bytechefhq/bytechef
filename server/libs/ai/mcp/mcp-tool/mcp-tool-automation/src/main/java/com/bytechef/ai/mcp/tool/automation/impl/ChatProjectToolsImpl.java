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

package com.bytechef.ai.mcp.tool.automation.impl;

import com.bytechef.ai.mcp.tool.automation.api.ChatProjectTools;
import com.bytechef.ai.mcp.tool.automation.api.ProjectTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChatProjectToolsImpl implements ChatProjectTools {
    private static final Logger logger = LoggerFactory.getLogger(ChatProjectToolsImpl.class);

    private final ProjectTools delegate;

    @SuppressFBWarnings("EI")
    public ChatProjectToolsImpl(ProjectTools projectTools) {
        this.delegate = projectTools;
    }


    @Override
    public List<ProjectToolsImpl.ProjectInfo> listProjects() {
        return delegate.listProjects();
    }

    @Override
    public ProjectToolsImpl.ProjectDetailInfo getProject(long projectId) {
        return delegate.getProject(projectId);
    }

    @Override
    public List<ProjectToolsImpl.ProjectInfo> searchProjects(String query) {
        return delegate.searchProjects(query);
    }

    @Override
    public ProjectToolsImpl.ProjectStatusInfo getProjectStatus(long projectId) {
        return delegate.getProjectStatus(projectId);
    }
}
