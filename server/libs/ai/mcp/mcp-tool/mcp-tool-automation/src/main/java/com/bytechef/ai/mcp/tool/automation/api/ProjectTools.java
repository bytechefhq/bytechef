package com.bytechef.ai.mcp.tool.automation.api;

import com.bytechef.ai.mcp.tool.automation.impl.ProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectWorkflowToolsImpl;

import java.util.List;

public interface ProjectTools extends ChatProjectTools {
    ProjectToolsImpl.ProjectInfo createProject(String name, String description, Long categoryId, Long workspaceId, List<Long> tagIds);
    String deleteProject(long projectId);
    ProjectToolsImpl.ProjectInfo updateProject(long projectId, String name, String description, Long categoryId, List<Long> tagIds);
    ProjectToolsImpl.ProjectPublishInfo publishProject(long projectId, String description);
}
