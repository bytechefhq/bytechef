package com.bytechef.ai.mcp.tool.automation.api;

import com.bytechef.ai.mcp.tool.automation.impl.ProjectWorkflowToolsImpl;

public interface ProjectWorkflowTools extends ChatProjectWorkflowTools{
    ProjectWorkflowToolsImpl.ProjectWorkflowInfo createProjectWorkflow(long projectId, String definition);
    String deleteWorkflow(String workflowId);
    ProjectWorkflowToolsImpl.WorkflowInfo updateWorkflow(String workflowId, String definition);
}
