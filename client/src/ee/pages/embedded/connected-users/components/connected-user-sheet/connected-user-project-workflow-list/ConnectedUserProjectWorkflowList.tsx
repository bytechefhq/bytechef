import ConnectedUserProjectWorkflowListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {ConnectedUserProjectWorkflow} from '@/shared/middleware/graphql';

const ConnectedUserProjectWorkflowList = ({
    connectedUserProjectWorkflows,
}: {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflow[];
}) => {
    return (
        <ul>
            {connectedUserProjectWorkflows.map((connectedUserProjectWorkflow) => (
                <ConnectedUserProjectWorkflowListItem
                    connectedUserProjectWorkflow={connectedUserProjectWorkflow}
                    key={connectedUserProjectWorkflow.id}
                />
            ))}
        </ul>
    );
};

export default ConnectedUserProjectWorkflowList;
