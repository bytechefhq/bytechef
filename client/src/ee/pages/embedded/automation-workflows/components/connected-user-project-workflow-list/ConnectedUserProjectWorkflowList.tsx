import ConnectedUserProjectWorkflowListItem from '@/ee/pages/embedded/automation-workflows/components/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {ConnectedUserProjectWorkflow} from '@/shared/middleware/graphql';

const ConnectedUserProjectWorkflowList = ({
    connectedUserProjectWorkflows,
}: {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflow[];
}) => {
    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <h3 className="heading-tertiary flex justify-start pl-2 text-sm">Workflows</h3>

            <ul className="divide-y divide-gray-100">
                {connectedUserProjectWorkflows.map((connectedUserProjectWorkflow) => (
                    <ConnectedUserProjectWorkflowListItem
                        connectedUserProjectWorkflow={connectedUserProjectWorkflow}
                        key={connectedUserProjectWorkflow.id}
                    />
                ))}
            </ul>
        </div>
    );
};

export default ConnectedUserProjectWorkflowList;
