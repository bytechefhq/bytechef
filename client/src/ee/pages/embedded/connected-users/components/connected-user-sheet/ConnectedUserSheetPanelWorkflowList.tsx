import ConnectedUserProjectWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList';
import {ConnectedUserWorkflowTemplateSourceI} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {useAutomationWorkflowProjectsQuery, useConnectedUserProjectsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

const ConnectedUserSheetPanelWorkflowList = ({connectedUserId}: {connectedUserId: number}) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data, isLoading} = useConnectedUserProjectsQuery({
        connectedUserId: connectedUserId.toString(),
        environmentId: currentEnvironmentId?.toString(),
    });

    const {data: automationWorkflowProjectsData, isLoading: automationWorkflowProjectsIsLoading} =
        useAutomationWorkflowProjectsQuery();

    const workflowTemplateSourcesByWorkflowUuid = useMemo(
        () =>
            new Map<string, ConnectedUserWorkflowTemplateSourceI>(
                (automationWorkflowProjectsData?.automationWorkflowProjects ?? []).flatMap(
                    (automationWorkflowProject) =>
                        automationWorkflowProject.workflowTemplates.map(
                            (workflowTemplate) =>
                                [
                                    workflowTemplate.workflowUuid,
                                    {projectName: automationWorkflowProject.name, workflowTemplate},
                                ] as const
                        )
                )
            ),
        [automationWorkflowProjectsData]
    );

    if (isLoading || automationWorkflowProjectsIsLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const connectedUserProjectWorkflows = (data?.connectedUserProjects ?? []).flatMap(
        (connectedUserProject) => connectedUserProject.connectedUserProjectWorkflows
    );

    return connectedUserProjectWorkflows.length > 0 ? (
        <ConnectedUserProjectWorkflowList
            connectedUserProjectWorkflows={connectedUserProjectWorkflows}
            workflowTemplateSourcesByWorkflowUuid={workflowTemplateSourcesByWorkflowUuid}
        />
    ) : (
        <div className="py-4 text-sm">No automation workflows.</div>
    );
};

export default ConnectedUserSheetPanelWorkflowList;
