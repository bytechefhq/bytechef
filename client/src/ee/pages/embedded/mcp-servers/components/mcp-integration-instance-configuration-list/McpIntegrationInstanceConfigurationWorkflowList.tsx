import {McpIntegrationInstanceConfigurationWorkflow} from '@/shared/middleware/graphql';

import McpIntegrationInstanceConfigurationWorkflowListItem from './McpIntegrationInstanceConfigurationWorkflowListItem';

interface McpIntegrationInstanceConfigurationWorkflowListProps {
    componentName: string;
    mcpIntegrationInstanceConfigurationWorkflows?: Array<McpIntegrationInstanceConfigurationWorkflow | null> | null;
}

const McpIntegrationInstanceConfigurationWorkflowList = ({
    componentName,
    mcpIntegrationInstanceConfigurationWorkflows,
}: McpIntegrationInstanceConfigurationWorkflowListProps) => {
    const workflows =
        mcpIntegrationInstanceConfigurationWorkflows?.filter(
            (workflow): workflow is McpIntegrationInstanceConfigurationWorkflow => workflow !== null
        ) || [];

    if (workflows.length === 0) {
        return null;
    }

    return (
        <div className="flex flex-wrap gap-2 py-2">
            {workflows.map((workflow) => (
                <McpIntegrationInstanceConfigurationWorkflowListItem
                    componentName={componentName}
                    key={workflow.id}
                    mcpIntegrationInstanceConfigurationWorkflow={workflow}
                />
            ))}
        </div>
    );
};

export default McpIntegrationInstanceConfigurationWorkflowList;
