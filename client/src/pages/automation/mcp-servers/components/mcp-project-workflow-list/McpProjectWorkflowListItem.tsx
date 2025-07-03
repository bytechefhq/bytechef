import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import {McpProjectWorkflow} from '@/shared/middleware/graphql';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useState} from 'react';

import McpProjectWorkflowListItemDropdownMenu from './McpProjectWorkflowListItemDropdownMenu';

interface McpProjectWorkflowListItemProps {
    mcpProjectWorkflow: McpProjectWorkflow;
}

const McpProjectWorkflowListItem = ({mcpProjectWorkflow}: McpProjectWorkflowListItemProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflow} = useGetWorkflowQuery(mcpProjectWorkflow.projectDeploymentWorkflow?.workflowId!);

    return (
        <div className="flex w-full items-center space-x-3">
            <div className="flex-1">
                <div className="flex items-center justify-between">
                    <div>
                        <p className="text-sm font-medium text-gray-900">{mcpProjectWorkflow.workflow?.label}</p>
                    </div>

                    <div className="flex items-center gap-x-6 text-xs text-gray-500">
                        {mcpProjectWorkflow.lastModifiedDate ? (
                            <span>{`Modified at ${new Date(mcpProjectWorkflow.lastModifiedDate).toLocaleDateString()}`}</span>
                        ) : (
                            <span>-</span>
                        )}

                        <McpProjectWorkflowListItemDropdownMenu onEditClick={() => setShowEditWorkflowDialog(true)} />
                    </div>
                </div>
            </div>

            {showEditWorkflowDialog && mcpProjectWorkflow.projectDeploymentWorkflow && workflow && (
                <ProjectDeploymentEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    /* eslint-disable @typescript-eslint/no-explicit-any */
                    projectDeploymentWorkflow={mcpProjectWorkflow.projectDeploymentWorkflow as any}
                    workflow={workflow}
                />
            )}
        </div>
    );
};

export default McpProjectWorkflowListItem;
