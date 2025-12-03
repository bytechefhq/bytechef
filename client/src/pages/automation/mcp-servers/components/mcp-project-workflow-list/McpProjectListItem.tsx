import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {McpProject} from '@/shared/middleware/graphql';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import McpProjectListItemDropdownMenu from './McpProjectListItemDropdownMenu';

interface McpProjectListItemProps {
    mcpProject: McpProject;
}

const McpProjectListItem = ({mcpProject}: McpProjectListItemProps) => {
    const [showUpdateProjectVersionDialog, setShowUpdateProjectVersionDialog] = useState(false);

    const {data: projectDeployment} = useGetProjectDeploymentQuery(+mcpProject.projectDeploymentId!);

    const queryClient = useQueryClient();

    const handleOnProjectDeploymentDialogClose = () => {
        queryClient
            .invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            })
            .then(() => setShowUpdateProjectVersionDialog(false));
    };

    return (
        <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
            <div className="flex flex-1 items-center py-1">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center justify-between">
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
                        onUpdateProjectVersionClick={() => setShowUpdateProjectVersionDialog(true)}
                    />
                </div>
            </div>

            {showUpdateProjectVersionDialog && (
                <ProjectDeploymentDialog
                    onClose={handleOnProjectDeploymentDialogClose}
                    projectDeployment={projectDeployment}
                    updateProjectVersion={true}
                />
            )}
        </div>
    );
};

export default McpProjectListItem;
