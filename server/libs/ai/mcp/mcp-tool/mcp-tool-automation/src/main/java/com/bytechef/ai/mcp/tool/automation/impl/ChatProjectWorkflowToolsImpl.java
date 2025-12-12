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

import com.bytechef.ai.mcp.tool.automation.api.ChatProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.api.ProjectWorkflowTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The ProjectWorkflowTools class provides utility methods and components to facilitate the management and execution of
 * project workflows.
 *
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
@Component
public class ChatProjectWorkflowToolsImpl implements ChatProjectWorkflowTools {

    private static final Logger logger = LoggerFactory.getLogger(ChatProjectWorkflowToolsImpl.class);

    private final ProjectWorkflowTools delegate;

    @SuppressFBWarnings("EI")
    public ChatProjectWorkflowToolsImpl(ProjectWorkflowTools projectTools) {
        this.delegate = projectTools;
    }

    @Override
    public ProjectWorkflowToolsImpl.WorkflowInfo getWorkflow(String workflowId) {
        return delegate.getWorkflow(workflowId);
    }

    @Override
    public String getWorkflowBuildInstructions() {
        return delegate.getWorkflowBuildInstructions();
    }

    @Override
    public List<ProjectWorkflowToolsImpl.WorkflowInfo> listWorkflows(long projectId) {
        return delegate.listWorkflows(projectId);
    }

    @Override
    public List<ProjectWorkflowToolsImpl.WorkflowInfo> searchWorkflows(String query, Long projectId) {
        return delegate.searchWorkflows(query, projectId);
    }

    @Override
    public ProjectWorkflowToolsImpl.WorkflowValidationResult validateWorkflow(String workflowId) {
        return delegate.validateWorkflow(workflowId);
    }
}
