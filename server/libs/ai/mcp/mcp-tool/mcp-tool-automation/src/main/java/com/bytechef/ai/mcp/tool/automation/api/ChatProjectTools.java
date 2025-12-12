package com.bytechef.ai.mcp.tool.automation.api;

import com.bytechef.ai.mcp.tool.automation.impl.ChatProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ProjectToolsImpl;

import java.util.List;

public interface ChatProjectTools {
    List<ProjectToolsImpl.ProjectInfo> listProjects();
    ProjectToolsImpl.ProjectDetailInfo getProject(long projectId);
    List<ProjectToolsImpl.ProjectInfo> searchProjects(String query);
    ProjectToolsImpl.ProjectStatusInfo getProjectStatus(long projectId);
}
