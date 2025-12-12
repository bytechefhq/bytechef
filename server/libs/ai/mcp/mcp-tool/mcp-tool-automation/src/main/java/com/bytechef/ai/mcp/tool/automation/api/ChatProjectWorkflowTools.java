package com.bytechef.ai.mcp.tool.automation.api;

import com.bytechef.ai.mcp.tool.automation.impl.ProjectWorkflowToolsImpl;

import java.util.List;

public interface ChatProjectWorkflowTools {
    ProjectWorkflowToolsImpl.WorkflowInfo getWorkflow(String workflowId);
    String getWorkflowBuildInstructions();
    List<ProjectWorkflowToolsImpl.WorkflowInfo> listWorkflows(long projectId);
    List<ProjectWorkflowToolsImpl.WorkflowInfo> searchWorkflows(String query, Long projectId);
    ProjectWorkflowToolsImpl.WorkflowValidationResult validateWorkflow(String workflowId);
}
