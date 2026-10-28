import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
    ConnectedUserWorkflowTemplateSourceI,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {useMemo} from 'react';

const OTHER_GROUP_NAME = 'Other';

interface ConnectedUserProjectWorkflowGroupI {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
    projectName?: string;
}

const getLastExecutionDate = (connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[]) => {
    const executionTimes = connectedUserProjectWorkflows
        .map((connectedUserProjectWorkflow) => connectedUserProjectWorkflow.lastExecutionDate)
        .filter((lastExecutionDate): lastExecutionDate is string => lastExecutionDate != null)
        .map((lastExecutionDate) => Date.parse(lastExecutionDate));

    return executionTimes.length > 0 ? new Date(Math.max(...executionTimes)) : undefined;
};

const ConnectedUserProjectWorkflowList = ({
    connectedUserProjectWorkflows,
    workflowTemplateSourcesByWorkflowUuid,
}: {
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
    workflowTemplateSourcesByWorkflowUuid: Map<string, ConnectedUserWorkflowTemplateSourceI>;
}) => {
    const groups = useMemo(() => {
        const groupsByProjectName = new Map<string, ConnectedUserProjectWorkflowGroupI>();

        for (const connectedUserProjectWorkflow of connectedUserProjectWorkflows) {
            const sourceWorkflowUuid =
                connectedUserProjectWorkflow.automationWorkflowUuid ??
                connectedUserProjectWorkflow.copiedFromWorkflowUuid;

            const projectName = sourceWorkflowUuid
                ? workflowTemplateSourcesByWorkflowUuid.get(sourceWorkflowUuid)?.projectName
                : undefined;

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
    }, [connectedUserProjectWorkflows, workflowTemplateSourcesByWorkflowUuid]);

    return (
        <div>
            {groups.map((group) => {
                const groupName = group.projectName ?? OTHER_GROUP_NAME;
                const workflowCount = group.connectedUserProjectWorkflows.length;
                const lastExecutionDate = getLastExecutionDate(group.connectedUserProjectWorkflows);

                return (
                    <Collapsible asChild key={groupName}>
                        <section aria-label={groupName} className="mb-2 rounded border border-border/50">
                            <div className="flex items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover">
                                <CollapsibleTrigger className="flex-1 py-3 text-left">
                                    <div className="flex flex-col items-start justify-center gap-y-2">
                                        <h3 className="flex min-h-8 items-center text-base font-semibold">
                                            {groupName}
                                        </h3>

                                        <div className="flex min-h-7 items-center text-xs font-semibold text-muted-foreground">
                                            {workflowCount === 1 ? '1 workflow' : `${workflowCount} workflows`}
                                        </div>
                                    </div>
                                </CollapsibleTrigger>

                                <div className="flex items-center gap-x-2">
                                    <div className="flex min-w-52 flex-col items-end justify-center gap-y-2">
                                        <Tooltip>
                                            <TooltipTrigger className="flex min-h-7 items-center text-xs text-content-neutral-secondary">
                                                {lastExecutionDate
                                                    ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                                    : 'No executions'}
                                            </TooltipTrigger>

                                            <TooltipContent side="left">Last Execution Date</TooltipContent>
                                        </Tooltip>
                                    </div>

                                    <div aria-hidden="true" className="size-9 shrink-0" />
                                </div>
                            </div>

                            <CollapsibleContent>
                                <div className="flex w-full flex-col gap-y-1 pt-3">
                                    <h4 className="flex justify-start px-3 text-sm font-semibold text-muted-foreground uppercase">
                                        Workflows
                                    </h4>

                                    <ul className="divide-y divide-stroke-neutral-primary">
                                        {group.connectedUserProjectWorkflows.map((connectedUserProjectWorkflow) => {
                                            const sourceWorkflowUuid =
                                                connectedUserProjectWorkflow.automationWorkflowUuid ??
                                                connectedUserProjectWorkflow.copiedFromWorkflowUuid;

                                            const workflowTemplate = sourceWorkflowUuid
                                                ? workflowTemplateSourcesByWorkflowUuid.get(sourceWorkflowUuid)
                                                      ?.workflowTemplate
                                                : undefined;

                                            return (
                                                <ConnectedUserProjectWorkflowListItem
                                                    connectedUserProjectWorkflow={connectedUserProjectWorkflow}
                                                    key={connectedUserProjectWorkflow.id}
                                                    projectName={group.projectName}
                                                    workflowTemplate={workflowTemplate}
                                                />
                                            );
                                        })}
                                    </ul>
                                </div>
                            </CollapsibleContent>
                        </section>
                    </Collapsible>
                );
            })}
        </div>
    );
};

export default ConnectedUserProjectWorkflowList;
