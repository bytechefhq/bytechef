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
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author Marko Kriskovic
 */
public class ChatProjectToolsImpl implements ChatProjectTools {
    private final ProjectTools delegate;

    @SuppressFBWarnings("EI")
    public ChatProjectToolsImpl(ProjectTools projectTools) {
        this.delegate = projectTools;
    }

    @Override
    @Tool(
        description = "List all projects in ByteChef. Returns a list of projects with their basic information including id, name, description, and status.")
    public List<ProjectToolsImpl.ProjectInfo> listProjects() {
        return delegate.listProjects();
    }

    @Override
    @Tool(
        description = "Get comprehensive information about a specific project. Returns detailed project information including id, name, description, status, versions, and metadata.")
    public ProjectToolsImpl.ProjectDetailInfo getProject(
        @ToolParam(description = "The ID of the project to retrieve") long projectId) {
        return delegate.getProject(projectId);
    }

    @Override
    @Tool(
        description = "Full-text search across all projects. Returns a list of projects matching the search query in name or description.")
    public List<ProjectToolsImpl.ProjectInfo> searchProjects(
        @ToolParam(description = "The search query to match against project names and descriptions") String query) {
        return delegate.searchProjects(query);
    }

    @Override
    @Tool(
        description = "Get project deployment and execution status. Returns detailed status information including deployment environments and their states.")
    public ProjectToolsImpl.ProjectStatusInfo getProjectStatus(
        @ToolParam(description = "The ID of the project to get status for") long projectId) {
        return delegate.getProjectStatus(projectId);
    }
}
