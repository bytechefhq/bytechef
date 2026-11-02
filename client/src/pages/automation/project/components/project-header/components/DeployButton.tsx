import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {getDisabledControlTooltip} from '@/pages/automation/project/components/project-header/util/permission-tooltip-utils';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkspaceScopeState} from '@/shared/hooks/useHasWorkspaceScope';
import {Project} from '@/shared/middleware/automation/configuration';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {RocketIcon} from 'lucide-react';
import {MouseEvent, useEffect, useState} from 'react';

interface DeployButtonProps {
    onDeploymentSaved?: (projectDeploymentId: number, environmentId?: number) => void;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    project: Project;
}

const DeployButton = ({onDeploymentSaved, onOpenChange, open, project}: DeployButtonProps) => {
    const [openRefetchSettled, setOpenRefetchSettled] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const projectDeploymentsQuery = useGetWorkspaceProjectDeploymentsQuery(
        {
            id: currentWorkspaceId ?? 0,
            projectId: project.id ?? 0,
        },
        false
    );

    // The dialog behind this button needs BOTH scopes, so gating on either one alone fails open:
    // ProjectDeploymentFacadeImpl.createProjectDeployment is annotated hasPermission(#projectDeploymentDTO,
    // 'WORKFLOW_EDIT'), and the ProjectDeploymentServiceImpl.create it delegates to is annotated
    // hasPermission(#projectDeployment.projectId, 'Project', 'DEPLOYMENT_CREATE') (as is .update).
    // The built-in roles put both at EDITOR rank so they always co-occur, but a custom role is an arbitrary scope
    // set — a member granted only DEPLOYMENT_CREATE would otherwise fill in the whole dialog and be refused on submit.
    // The four-state hook is used instead of the plain boolean one so that "not loaded yet", "check failed" and "the
    // edition never resolved so nothing was ever asked" stay distinguishable from "denied"; see
    // getDisabledControlTooltip.
    const deploymentCreateState = useWorkspaceScopeState(currentWorkspaceId, 'DEPLOYMENT_CREATE');
    const workflowEditState = useWorkspaceScopeState(currentWorkspaceId, 'WORKFLOW_EDIT');

    const canDeployProject = deploymentCreateState.granted && workflowEditState.granted;
    const isDeployable = !!(project.lastPublishedDate && project.lastProjectVersion);
    const permissionsError = deploymentCreateState.error || workflowEditState.error;
    // An unresolved workspace id counts as "not yet known" rather than as a refusal, for the same reason.
    const permissionsLoading = currentWorkspaceId == null || deploymentCreateState.loading || workflowEditState.loading;
    const permissionsUnknown = deploymentCreateState.editionUnknown || workflowEditState.editionUnknown;

    const handleDeployClick = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (!currentWorkspaceId || !project.id) {
            return;
        }

        await projectDeploymentsQuery.refetch();
    };

    const refetchProjectDeployments = projectDeploymentsQuery.refetch;

    useEffect(() => {
        if (!open) {
            setOpenRefetchSettled(false);

            return;
        }

        if (!isDeployable) {
            onOpenChange?.(false);

            return;
        }

        if (!currentWorkspaceId || !project.id) {
            return;
        }

        let cancelled = false;

        void refetchProjectDeployments().finally(() => {
            if (!cancelled) {
                setOpenRefetchSettled(true);
            }
        });

        return () => {
            cancelled = true;
        };
    }, [currentWorkspaceId, isDeployable, onOpenChange, open, project.id, refetchProjectDeployments]);

    if (!canDeployProject || !isDeployable) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="inline-flex">
                        <Button
                            className="rounded-none border-l-0"
                            disabled
                            icon={<RocketIcon />}
                            label="Deploy"
                            variant="outline"
                        />
                    </span>
                </TooltipTrigger>

                <TooltipContent>
                    {getDisabledControlTooltip({
                        deniedMessage: 'You do not have permission to deploy this project',
                        granted: canDeployProject,
                        permissionsError,
                        permissionsLoading,
                        permissionsUnknown,
                        unmetPreconditionMessage: 'Publish the project before deploying it.',
                    })}
                </TooltipContent>
            </Tooltip>
        );
    }

    return (
        <Tooltip>
            <ProjectDeploymentDialog
                onSuccess={onDeploymentSaved}
                projectDeployment={{
                    name: project.name,
                    projectId: project.id,
                }}
                projectDeployments={projectDeploymentsQuery.data}
                projectDeploymentsLoading={projectDeploymentsQuery.isFetching}
                redirectOnSubmit={false}
                showTabs
                triggerNode={
                    <TooltipTrigger asChild>
                        <Button icon={<RocketIcon />} label="Deploy" onClick={handleDeployClick} variant="outline" />
                    </TooltipTrigger>
                }
            />

            <TooltipContent>Deploy the published project version</TooltipContent>

            {open && openRefetchSettled && (
                <ProjectDeploymentDialog
                    onClose={() => onOpenChange?.(false)}
                    onSuccess={onDeploymentSaved}
                    projectDeployment={{
                        name: project.name,
                        projectId: project.id,
                    }}
                    projectDeployments={projectDeploymentsQuery.data}
                    projectDeploymentsLoading={projectDeploymentsQuery.isFetching}
                    redirectOnSubmit={false}
                    showTabs
                />
            )}
        </Tooltip>
    );
};

export default DeployButton;
