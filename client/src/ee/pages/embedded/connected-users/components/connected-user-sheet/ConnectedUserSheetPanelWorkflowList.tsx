import Button from '@/components/Button/Button';
import ConnectedUserProjectWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList';
import {ConnectedUserWorkflowTemplateSourceI} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import ProvisionProjectWorkflowDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-project-workflow-dialog/ProvisionProjectWorkflowDialog';
import {useAutomationProjectsQuery, useConnectedUserProjectsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PlusIcon} from 'lucide-react';
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

    const workflowTemplateSourcesByWorkflowUuid = useMemo(
        () =>
            new Map<string, ConnectedUserWorkflowTemplateSourceI>(
                (automationProjectsData?.automationProjects ?? []).flatMap((automationProject) =>
                    automationProject.workflowTemplates.map(
                        (workflowTemplate) =>
                            [
                                workflowTemplate.workflowUuid,
                                {projectName: automationProject.name, workflowTemplate},
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

    const connectedUserProjectWorkflows = (data?.connectedUserProjects ?? []).flatMap(
        (connectedUserProject) => connectedUserProject.connectedUserProjectWorkflows
    );

    return (
        <div>
            <div className="flex items-center justify-between pb-2">
                <h3 className="text-sm font-semibold">Automation Workflows</h3>

                <Button
                    icon={<PlusIcon />}
                    label="New Workflow"
                    onClick={() => setShowProvisionDialog(true)}
                    size="sm"
                    variant="secondary"
                />
            </div>

            {connectedUserProjectWorkflows.length > 0 ? (
                <ConnectedUserProjectWorkflowList
                    configurations={configurations}
                    connectedUser={connectedUser}
                    connectedUserProjectWorkflows={connectedUserProjectWorkflows}
                    workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
                />
            ) : (
                <div className="py-4 text-sm">No automation workflows.</div>
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
