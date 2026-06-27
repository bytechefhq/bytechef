import McpIntegrationInstanceConfigurationWorkflowListItem from './McpIntegrationInstanceConfigurationWorkflowListItem';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from './hooks/useMcpIntegrationInstanceConfigurationList';

interface McpIntegrationInstanceConfigurationWorkflowListProps {
    componentName: string;
    mcpIntegrationInstanceConfigurationWorkflows?: Array<McpIntegrationInstanceConfigurationWorkflowItemType | null> | null;
}

const McpIntegrationInstanceConfigurationWorkflowList = ({
    componentName,
    mcpIntegrationInstanceConfigurationWorkflows,
}: McpIntegrationInstanceConfigurationWorkflowListProps) => {
    const workflows =
        mcpIntegrationInstanceConfigurationWorkflows?.filter(
            (workflow): workflow is McpIntegrationInstanceConfigurationWorkflowItemType => workflow !== null
        ) || [];

    if (workflows.length === 0) {
        return null;
    }

    return (
        <div className="flex flex-col gap-1">
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
