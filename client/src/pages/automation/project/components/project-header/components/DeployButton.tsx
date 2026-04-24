import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project} from '@/shared/middleware/automation/configuration';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {RocketIcon} from 'lucide-react';
import {MouseEvent} from 'react';

const DeployButton = ({project}: {project: Project}) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const projectDeploymentsQuery = useGetWorkspaceProjectDeploymentsQuery(
        {
            id: currentWorkspaceId ?? 0,
            projectId: project.id ?? 0,
        },
        false
    );

    const isDeployable = !!(project.lastPublishedDate && project.lastProjectVersion);

    const handleDeployClick = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (!currentWorkspaceId || !project.id) {
            return;
        }

        await projectDeploymentsQuery.refetch();
    };

    if (!isDeployable) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="ml-2 inline-flex">
                        <Button disabled icon={<RocketIcon />} label="Deploy" variant="outline" />
                    </span>
                </TooltipTrigger>

                <TooltipContent>Publish the project to enable deployment</TooltipContent>
            </Tooltip>
        );
    }

    return (
        <ProjectDeploymentDialog
            environmentEditable={true}
            projectDeployment={{
                name: project.name,
                projectId: project.id,
            }}
            projectDeployments={projectDeploymentsQuery.data}
            projectDeploymentsLoading={projectDeploymentsQuery.isFetching}
            redirectOnSubmit={false}
            triggerNode={
                <Button
                    className="ml-2"
                    icon={<RocketIcon />}
                    label="Deploy"
                    onClick={handleDeployClick}
                    variant="outline"
                />
            }
        />
    );
};

export default DeployButton;
