import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {McpProject} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';

import McpProjectWorkflowDialog from '../McpProjectWorkflowDialog';
import McpProjectListItemDropdownMenu from './McpProjectListItemDropdownMenu';
import useMcpProjectListItem from './hooks/useMcpProjectListItem';

interface McpProjectListItemProps {
    mcpProject: McpProject;
}

const McpProjectListItem = ({mcpProject}: McpProjectListItemProps) => {
    const {
        handleOnProjectDeploymentDialogClose,
        mcpWorkflowUuids,
        projectDeployment,
        setShowEditWorkflowsDialog,
        setShowUpdateProjectVersionDialog,
        showEditWorkflowsDialog,
        showUpdateProjectVersionDialog,
    } = useMcpProjectListItem(mcpProject);

    return (
        <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
            <div className="flex flex-1 items-center py-1">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center gap-x-2">
                            <WorkflowIcon className="size-4 flex-none text-gray-500" />

                            <span className="mr-2 text-base font-semibold">
                                {mcpProject.project?.name || `Project ${mcpProject.projectDeploymentId}`}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    {mcpProject.projectVersion && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`v${mcpProject.projectVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Project Version</TooltipContent>
                        </Tooltip>
                    )}

                    <div className="flex min-w-52 flex-col items-end gap-y-4">
                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {mcpProject.lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${new Date(mcpProject.lastModifiedDate).toLocaleDateString()} ${new Date(mcpProject.lastModifiedDate).toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Last Updated Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <McpProjectListItemDropdownMenu
                        mcpProject={mcpProject}
                        onEditWorkflowsClick={() => setShowEditWorkflowsDialog(true)}
                        onUpdateProjectVersionClick={() => setShowUpdateProjectVersionDialog(true)}
                    />
                </div>
            </div>

            {showEditWorkflowsDialog && (
                <McpProjectWorkflowDialog mcpProject={mcpProject} onClose={() => setShowEditWorkflowsDialog(false)} />
            )}

            {showUpdateProjectVersionDialog && (
                <ProjectDeploymentDialog
                    filterWorkflowUuids={mcpWorkflowUuids}
                    onClose={handleOnProjectDeploymentDialogClose}
                    projectDeployment={projectDeployment}
                    updateProjectVersion={true}
                />
            )}
        </div>
    );
};

export default McpProjectListItem;
