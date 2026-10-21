import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {getApiCollectionEndpointPath} from '@/ee/pages/automation/api-platform/api-collections/utils/getApiCollectionEndpointPath';
import {getHttpMethodBadgeStyles} from '@/ee/pages/automation/api-platform/api-collections/utils/getHttpMethodBadgeStyles';
import {useGetApiCollectionQuery} from '@/ee/shared/mutations/automation/apiCollections.queries';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import OpenInButton from '@/shared/components/OpenInButton';
import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {handleActivationKeyDown} from '@/shared/util/keyboard-utils';
import {EditIcon, EllipsisVerticalIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface AppApiCollectionWorkflowsProps {
    apiCollectionId: number;
    canEdit: boolean;
    projectDeploymentId: number;
    projectId: number;
    projectName?: string | null;
    projectVersion: number;
}

interface DeploymentWorkflowItemI {
    projectDeploymentWorkflow: ProjectDeploymentWorkflow;
    workflow: Workflow;
}

const AppApiCollectionWorkflows = ({
    apiCollectionId,
    canEdit,
    projectDeploymentId,
    projectId,
    projectName,
    projectVersion,
}: AppApiCollectionWorkflowsProps) => {
    const [editedItem, setEditedItem] = useState<DeploymentWorkflowItemI | undefined>();

    const {openProjectWorkflow} = useOpenInProject();

    const openProjectDeploymentWorkflowSheet = useProjectDeploymentWorkflowSheetStore(
        (state) => state.openProjectDeploymentWorkflowSheet
    );

    const {data: apiCollection} = useGetApiCollectionQuery(apiCollectionId);
    const {data: projectDeployment} = useGetProjectDeploymentQuery(projectDeploymentId);
    const {data: workflows} = useGetProjectVersionWorkflowsQuery(projectId, projectVersion);

    const deploymentWorkflowItemsByUuid = useMemo(
        () =>
            new Map(
                (projectDeployment?.projectDeploymentWorkflows ?? []).flatMap((projectDeploymentWorkflow) => {
                    const workflow = workflows?.find(
                        (projectWorkflow) => projectWorkflow.workflowUuid === projectDeploymentWorkflow.workflowUuid
                    );

                    return workflow && projectDeploymentWorkflow.workflowUuid
                        ? [[projectDeploymentWorkflow.workflowUuid, {projectDeploymentWorkflow, workflow}] as const]
                        : [];
                })
            ),
        [projectDeployment?.projectDeploymentWorkflows, workflows]
    );

    const apiCollectionEndpoints = apiCollection?.endpoints ?? [];

    return (
        <div className="space-y-2 py-2 pl-6">
            <h3 className="text-sm font-semibold text-content-neutral-tertiary uppercase">Endpoints</h3>

            {apiCollectionEndpoints.length > 0 ? (
                <ul className="divide-y divide-stroke-neutral-primary">
                    {apiCollectionEndpoints.map((apiCollectionEndpoint) => {
                        const {icon, textColor} = getHttpMethodBadgeStyles(apiCollectionEndpoint.httpMethod);
                        const deploymentWorkflowItem = apiCollectionEndpoint.workflowUuid
                            ? deploymentWorkflowItemsByUuid.get(apiCollectionEndpoint.workflowUuid)
                            : undefined;

                        return (
                            <li
                                className="-mx-2 flex items-center justify-between rounded-md px-2 py-2 text-sm hover:bg-surface-neutral-primary-hover"
                                key={apiCollectionEndpoint.id}
                            >
                                <div
                                    className={twMerge(
                                        'flex flex-1 items-center',
                                        deploymentWorkflowItem && 'cursor-pointer'
                                    )}
                                    onClick={
                                        deploymentWorkflowItem
                                            ? () =>
                                                  openProjectDeploymentWorkflowSheet({
                                                      projectDeploymentId,
                                                      projectId,
                                                      projectName: projectName ?? undefined,
                                                      projectVersion,
                                                      workflow: deploymentWorkflowItem.workflow,
                                                  })
                                            : undefined
                                    }
                                    onKeyDown={deploymentWorkflowItem ? handleActivationKeyDown : undefined}
                                    role={deploymentWorkflowItem ? 'button' : undefined}
                                    tabIndex={deploymentWorkflowItem ? 0 : undefined}
                                >
                                    <Badge
                                        className={twMerge('mr-4 w-20', textColor)}
                                        icon={icon}
                                        label={apiCollectionEndpoint.httpMethod}
                                        styleType="outline-outline"
                                        weight="semibold"
                                    />

                                    <span className="flex w-4/12 items-center gap-2">
                                        {apiCollectionEndpoint.name}

                                        {deploymentWorkflowItem && (
                                            <OpenInButton
                                                label="Open in project"
                                                onClick={() =>
                                                    void openProjectWorkflow(
                                                        projectId,
                                                        deploymentWorkflowItem.workflow.workflowUuid
                                                    )
                                                }
                                            />
                                        )}
                                    </span>

                                    <span className="font-semibold">
                                        {getApiCollectionEndpointPath(
                                            apiCollection?.collectionVersion,
                                            apiCollection?.contextPath,
                                            apiCollectionEndpoint.path
                                        )}
                                    </span>
                                </div>

                                {canEdit && deploymentWorkflowItem && (
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button
                                                aria-label={`${apiCollectionEndpoint.name} actions`}
                                                icon={<EllipsisVerticalIcon />}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end" className="p-0">
                                            <DropdownMenuItem
                                                className="dropdown-menu-item"
                                                onClick={() => setEditedItem(deploymentWorkflowItem)}
                                            >
                                                <EditIcon /> Edit Workflow
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                )}
                            </li>
                        );
                    })}
                </ul>
            ) : (
                <p className="text-sm text-content-neutral-secondary">This collection has no endpoints yet.</p>
            )}

            {editedItem && (
                <ProjectDeploymentEditWorkflowDialog
                    onClose={() => setEditedItem(undefined)}
                    projectDeploymentWorkflow={editedItem.projectDeploymentWorkflow}
                    workflow={editedItem.workflow}
                />
            )}
        </div>
    );
};

export default AppApiCollectionWorkflows;
