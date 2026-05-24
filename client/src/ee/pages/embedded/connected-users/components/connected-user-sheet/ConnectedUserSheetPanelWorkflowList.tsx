import ConnectedUserProjectWorkflowList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList';
import {useConnectedUserProjectsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

const ConnectedUserSheetPanelWorkflowList = ({connectedUserId}: {connectedUserId: number}) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data, isLoading} = useConnectedUserProjectsQuery({
        connectedUserId: connectedUserId.toString(),
        environmentId: currentEnvironmentId?.toString(),
    });

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const connectedUserProjectWorkflows = (data?.connectedUserProjects ?? []).flatMap(
        (connectedUserProject) => connectedUserProject.connectedUserProjectWorkflows
    );

    return connectedUserProjectWorkflows.length > 0 ? (
        <ConnectedUserProjectWorkflowList connectedUserProjectWorkflows={connectedUserProjectWorkflows} />
    ) : (
        <div className="py-4 text-sm">No automation workflows.</div>
    );
};

export default ConnectedUserSheetPanelWorkflowList;
