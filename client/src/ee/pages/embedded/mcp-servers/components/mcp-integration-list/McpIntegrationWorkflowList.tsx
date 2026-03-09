import {McpIntegrationWorkflow} from '@/shared/middleware/graphql';

import McpIntegrationWorkflowListItem from './McpIntegrationWorkflowListItem';

interface McpIntegrationWorkflowListProps {
    componentName: string;
    mcpIntegrationWorkflows?: Array<McpIntegrationWorkflow | null> | null;
}

const McpIntegrationWorkflowList = ({componentName, mcpIntegrationWorkflows}: McpIntegrationWorkflowListProps) => {
    const workflows =
        mcpIntegrationWorkflows?.filter((workflow): workflow is McpIntegrationWorkflow => workflow !== null) || [];

    if (workflows.length === 0) {
        return null;
    }

    return (
        <div className="flex flex-wrap gap-2 py-2">
            {workflows.map((workflow) => (
                <McpIntegrationWorkflowListItem
                    componentName={componentName}
                    key={workflow.id}
                    mcpIntegrationWorkflow={workflow}
                />
            ))}
        </div>
    );
};

export default McpIntegrationWorkflowList;
