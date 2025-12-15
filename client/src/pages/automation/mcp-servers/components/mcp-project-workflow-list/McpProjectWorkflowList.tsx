import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {McpProjectWorkflow} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';

import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';

interface McpProjectWorkflowListProps {
    mcpProjectWorkflows?: Array<McpProjectWorkflow | null> | null;
}

const McpProjectWorkflowList = ({mcpProjectWorkflows}: McpProjectWorkflowListProps) => {
    const workflows =
        mcpProjectWorkflows?.filter((workflow): workflow is McpProjectWorkflow => workflow !== null) || [];

    return (
        <div className="py-3">
            {workflows.length > 0 ? (
                <ul className="divide-y divide-gray-100">
                    {workflows.map((workflow) => (
                        <li
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                            key={workflow.id}
                        >
                            <McpProjectWorkflowListItem mcpProjectWorkflow={workflow} />
                        </li>
                    ))}
                </ul>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={<Button disabled label="Create Workflow" />}
                        icon={<WorkflowIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a workflow for this MCP project."
                        title="No Workflows"
                    />
                </div>
            )}
        </div>
    );
};

export default McpProjectWorkflowList;
