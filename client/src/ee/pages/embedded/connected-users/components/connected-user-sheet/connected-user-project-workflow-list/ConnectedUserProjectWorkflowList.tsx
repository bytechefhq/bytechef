import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';

const ConnectedUserProjectWorkflowList = ({
    connectedUserProjectWorkflows,
}: {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
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
