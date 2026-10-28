import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {useMemo} from 'react';

const OTHER_GROUP_NAME = 'Other';

interface ConnectedUserProjectWorkflowGroupI {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
    projectName?: string;
}

const ConnectedUserProjectWorkflowList = ({
    connectedUserProjectWorkflows,
    projectNamesByWorkflowUuid,
}: {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
    projectNamesByWorkflowUuid: Map<string, string>;
}) => {
    const groups = useMemo(() => {
        const groupsByProjectName = new Map<string, ConnectedUserProjectWorkflowGroupI>();

        for (const connectedUserProjectWorkflow of connectedUserProjectWorkflows) {
            const sourceWorkflowUuid =
                connectedUserProjectWorkflow.automationWorkflowUuid ??
                connectedUserProjectWorkflow.copiedFromWorkflowUuid;

            const projectName = sourceWorkflowUuid ? projectNamesByWorkflowUuid.get(sourceWorkflowUuid) : undefined;

            const groupKey = projectName ?? OTHER_GROUP_NAME;

            const group = groupsByProjectName.get(groupKey) ?? {connectedUserProjectWorkflows: [], projectName};

            group.connectedUserProjectWorkflows.push(connectedUserProjectWorkflow);

            groupsByProjectName.set(groupKey, group);
        }

        return [...groupsByProjectName.values()].sort((firstGroup, secondGroup) => {
            if (!firstGroup.projectName || !secondGroup.projectName) {
                return firstGroup.projectName ? -1 : 1;
            }

            return firstGroup.projectName.localeCompare(secondGroup.projectName);
        });
    }, [connectedUserProjectWorkflows, projectNamesByWorkflowUuid]);

    return (
        <div className="flex flex-col gap-y-4">
            {groups.map((group) => (
                <section aria-label={group.projectName ?? OTHER_GROUP_NAME} key={group.projectName ?? OTHER_GROUP_NAME}>
                    <h3 className="px-3 pb-2 text-sm heading-tertiary">{group.projectName ?? OTHER_GROUP_NAME}</h3>

                    <ul>
                        {group.connectedUserProjectWorkflows.map((connectedUserProjectWorkflow) => (
                            <ConnectedUserProjectWorkflowListItem
                                connectedUserProjectWorkflow={connectedUserProjectWorkflow}
                                key={connectedUserProjectWorkflow.id}
                                projectName={group.projectName}
                            />
                        ))}
                    </ul>
                </section>
            ))}
        </div>
    );
};

export default ConnectedUserProjectWorkflowList;
