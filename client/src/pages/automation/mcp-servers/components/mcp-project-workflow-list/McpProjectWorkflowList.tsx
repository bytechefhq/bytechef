import {McpProjectWorkflow} from '@/shared/middleware/graphql';

import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';

interface McpProjectWorkflowListProps {
    mcpProjectWorkflows?: Array<McpProjectWorkflow | null> | null;
}

const McpProjectWorkflowList = ({mcpProjectWorkflows}: McpProjectWorkflowListProps) => {
    const workflows =
        mcpProjectWorkflows?.filter((workflow): workflow is McpProjectWorkflow => workflow !== null) || [];

    if (workflows.length === 0) {
        return null;
    }

    return (
        <div className="flex flex-wrap gap-2 py-2">
            {workflows.map((workflow) => (
                <McpProjectWorkflowListItem key={workflow.id} mcpProjectWorkflow={workflow} />
            ))}
        </div>
    );
};

export default McpProjectWorkflowList;
