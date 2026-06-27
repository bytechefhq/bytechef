import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';
import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';

interface McpProjectWorkflowListProps {
    mcpProjectWorkflows?: Array<McpProjectWorkflowItemType | null> | null;
}

const McpProjectWorkflowList = ({mcpProjectWorkflows}: McpProjectWorkflowListProps) => {
    const workflows =
        mcpProjectWorkflows?.filter((workflow): workflow is NonNullable<typeof workflow> => workflow !== null) || [];

    if (workflows.length === 0) {
        return null;
    }

    return (
        <div className="flex flex-col gap-1">
            {workflows.map((workflow) => (
                <McpProjectWorkflowListItem key={workflow.id} mcpProjectWorkflow={workflow} />
            ))}
        </div>
    );
};

export default McpProjectWorkflowList;
