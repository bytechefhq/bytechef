import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserSheetEmptyState from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetEmptyState';
import ConnectedUserProjectWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList';
import {ConnectedUserWorkflowTemplateSourceI} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import ProvisionProjectWorkflowDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog/ProvisionProjectWorkflowDialog';
import {
    useAutomationProjectsQuery,
    useConnectedUserProjectsQuery,
    useProvisionableConnectedUserProjectsQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {FolderIcon, PlusIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

interface ConnectedUserSheetPanelWorkflowListProps {
    connectedUserEmail?: string;
    connectedUserId: number;
    connectedUserName?: string;
}

const ConnectedUserSheetPanelWorkflowList = ({
    connectedUserEmail,
    connectedUserId,
    connectedUserName,
}: ConnectedUserSheetPanelWorkflowListProps) => {
    const [showProvisionDialog, setShowProvisionDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data, isLoading} = useConnectedUserProjectsQuery({
        connectedUserId: connectedUserId.toString(),
        environmentId: currentEnvironmentId?.toString(),
    });

    const {data: automationProjectsData, isLoading: automationProjectsIsLoading} = useAutomationProjectsQuery();

    const {data: provisionableData} = useProvisionableConnectedUserProjectsQuery({
        connectedUserId: connectedUserId.toString(),
    });

    const workflowTemplateSourcesByWorkflowUuid = useMemo(
        () =>
            new Map<string, ConnectedUserWorkflowTemplateSourceI>(
                (automationProjectsData?.automationProjects ?? []).flatMap((automationProject) =>
                    automationProject.workflowTemplates.map(
                        (workflowTemplate) =>
                            [
                                workflowTemplate.workflowUuid,
                                {
                                    projectId: automationProject.id,
                                    projectName: automationProject.name,
                                    workflowTemplate,
                                },
                            ] as const
                    )
                )
            ),
        [automationProjectsData]
    );

    if (isLoading || automationProjectsIsLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const connectedUserProjects = data?.connectedUserProjects ?? [];

    const configurations = connectedUserProjects.flatMap(
        (connectedUserProject) => connectedUserProject.automationProjectConfigurations
    );

    const connectedUser = {
        connectedUserId: connectedUserId.toString(),
        email: connectedUserEmail,
        externalId: connectedUserProjects[0]?.connectedUser.externalId || connectedUserId.toString(),
        name: connectedUserName,
    };

    const nothingToProvision = provisionableData?.provisionableConnectedUserProjects.length === 0;

    const connectedUserProjectWorkflows = (data?.connectedUserProjects ?? []).flatMap(
        (connectedUserProject) => connectedUserProject.connectedUserProjectWorkflows
    );

    return (
        <div>
            {connectedUserProjectWorkflows.length > 0 ? (
                <>
                    <div className="flex items-center justify-end pb-2">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <span className="block">
                                    <Button
                                        disabled={nothingToProvision}
                                        icon={<PlusIcon />}
                                        label="Provision Workflow"
                                        onClick={() => setShowProvisionDialog(true)}
                                        size="sm"
                                        variant="secondary"
                                    />
                                </span>
                            </TooltipTrigger>

                            {nothingToProvision && (
                                <TooltipContent>
                                    No project has a configuration released to this user's environment yet.
                                </TooltipContent>
                            )}
                        </Tooltip>
                    </div>

                    <ConnectedUserProjectWorkflowList
                        configurations={configurations}
                        connectedUser={connectedUser}
                        connectedUserProjectWorkflows={connectedUserProjectWorkflows}
                        workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
                    />
                </>
            ) : (
                <ConnectedUserSheetEmptyState
                    description="Provision a workflow from one of your automation projects. This user gets their own copy to adjust."
                    icon={<FolderIcon />}
                    nothingToProvision={nothingToProvision}
                    nothingToProvisionDescription="No project has a configuration released to this user's environment yet. Release one first, then provision its workflows for this user."
                    onProvision={() => setShowProvisionDialog(true)}
                    pageLink={{label: 'Go to Automations', to: '/embedded/automations'}}
                    provisionLabel="Provision Workflow"
                    title="No automations yet"
                />
            )}

            {showProvisionDialog && (
                <ProvisionProjectWorkflowDialog
                    assignedConfigurations={configurations}
                    connectedUserId={connectedUserId}
                    onClose={() => setShowProvisionDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectedUserSheetPanelWorkflowList;
