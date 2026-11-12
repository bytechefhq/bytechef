import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserProjectConfigurationMenu, {
    ConnectedUserCurrentConfigurationI,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectConfigurationMenu';
import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
    ConnectedUserWorkflowTemplateSourceI,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import ListGroupHeader from '@/shared/components/ListGroupHeader';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import isCollapsibleRowClick from '@/shared/components/mcp-server/utils/isCollapsibleRowClick';
import {FolderIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

const OTHER_GROUP_KEY = 'other';

const OTHER_GROUP_NAME = 'Created by this user';

interface ConnectedUserProjectWorkflowGroupI {
    configuration?: ConnectedUserCurrentConfigurationI & {projectId: string};
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
    key: string;
    projectIds: Set<string>;
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
    configurations,
    connectedUser,
    connectedUserProjectWorkflows,
    workflowTemplateSourcesByWorkflowUuid,
}: {
    configurations?: Array<ConnectedUserCurrentConfigurationI & {projectId: string}>;
    connectedUser?: {connectedUserId: string; email?: string | null; externalId: string; name?: string | null};
    connectedUserProjectWorkflows: ConnectedUserProjectWorkflowType[];
    workflowTemplateSourcesByWorkflowUuid: Map<string, ConnectedUserWorkflowTemplateSourceI>;
}) => {
    const [openGroupKeys, setOpenGroupKeys] = useState<Set<string>>(new Set());

    const groups = useMemo(() => {
        const groupsByKey = new Map<string, ConnectedUserProjectWorkflowGroupI>();

        for (const connectedUserProjectWorkflow of connectedUserProjectWorkflows) {
            const sourceWorkflowUuid =
                connectedUserProjectWorkflow.automationUuid ?? connectedUserProjectWorkflow.copiedFromWorkflowUuid;

            const workflowTemplateSource = sourceWorkflowUuid
                ? workflowTemplateSourcesByWorkflowUuid.get(sourceWorkflowUuid)
                : undefined;

            const projectId = workflowTemplateSource?.projectId ?? connectedUserProjectWorkflow.projectId;

            const groupKey = workflowTemplateSource ? `project-${projectId}` : OTHER_GROUP_KEY;

            const group = groupsByKey.get(groupKey) ?? {
                connectedUserProjectWorkflows: [],
                key: groupKey,
                projectIds: new Set<string>(),
                projectName: workflowTemplateSource?.projectName,
            };

            group.connectedUserProjectWorkflows.push(connectedUserProjectWorkflow);
            group.projectIds.add(projectId);

            groupsByKey.set(groupKey, group);
        }

        for (const group of groupsByKey.values()) {
            if (group.projectIds.size === 1) {
                const [groupProjectId] = group.projectIds;

                group.configuration = configurations?.find(
                    (configuration) => configuration.projectId === groupProjectId
                );
            }
        }

        return [...groupsByKey.values()].sort((firstGroup, secondGroup) => {
            if (!firstGroup.projectName || !secondGroup.projectName) {
                return firstGroup.projectName ? -1 : 1;
            }

            return (
                firstGroup.projectName.localeCompare(secondGroup.projectName) ||
                firstGroup.key.localeCompare(secondGroup.key)
            );
        });
    }, [configurations, connectedUserProjectWorkflows, workflowTemplateSourcesByWorkflowUuid]);

    const setGroupOpen = (groupKey: string, open: boolean) =>
        setOpenGroupKeys((previousOpenGroupKeys) => {
            const nextOpenGroupKeys = new Set(previousOpenGroupKeys);

            if (open) {
                nextOpenGroupKeys.add(groupKey);
            } else {
                nextOpenGroupKeys.delete(groupKey);
            }

            return nextOpenGroupKeys;
        });

    const renderWorkflowItems = (group: ConnectedUserProjectWorkflowGroupI) =>
        group.connectedUserProjectWorkflows.map((connectedUserProjectWorkflow) => {
            const sourceWorkflowUuid =
                connectedUserProjectWorkflow.automationUuid ?? connectedUserProjectWorkflow.copiedFromWorkflowUuid;

            const workflowTemplate = sourceWorkflowUuid
                ? workflowTemplateSourcesByWorkflowUuid.get(sourceWorkflowUuid)?.workflowTemplate
                : undefined;

            return (
                <ConnectedUserProjectWorkflowListItem
                    configurationProjectVersion={group.configuration?.configurationProjectVersion}
                    connectedUserProjectWorkflow={connectedUserProjectWorkflow}
                    key={connectedUserProjectWorkflow.id}
                    projectName={group.projectName}
                    workflowTemplate={workflowTemplate}
                />
            );
        });

    return (
        <div>
            {groups.map((group, groupIndex) => {
                const groupName = group.projectName ?? OTHER_GROUP_NAME;
                const groupOpen = openGroupKeys.has(group.key);
                const lastExecutionDate = getLastExecutionDate(group.connectedUserProjectWorkflows);
                const workflowCount = group.connectedUserProjectWorkflows.length;

                return (
                    <section aria-label={groupName} key={group.key}>
                        <ListGroupHeader
                            className={groupIndex === 0 ? 'pt-0' : undefined}
                            icon={<FolderIcon aria-hidden="true" className="size-5 shrink-0" />}
                            name={groupName}
                        />

                        <Collapsible
                            className="group rounded border border-border/50"
                            onOpenChange={(open) => setGroupOpen(group.key, open)}
                            open={groupOpen}
                        >
                            <div
                                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                                onClick={(event) => {
                                    if (isCollapsibleRowClick(event)) {
                                        setGroupOpen(group.key, !groupOpen);
                                    }
                                }}
                                role="presentation"
                            >
                                <ListItemExpandButton
                                    label={group.configuration ? 'Toggle configuration' : 'Toggle workflows'}
                                />

                                <div className="flex flex-1 flex-col items-start justify-center gap-y-2 py-3">
                                    {group.configuration ? (
                                        <span className="flex min-h-6 items-center text-base font-semibold">
                                            {group.configuration.configurationName}
                                        </span>
                                    ) : (
                                        <span className="flex min-h-6 items-center text-base font-semibold text-content-neutral-secondary">
                                            No configuration
                                        </span>
                                    )}

                                    <span className="flex min-h-6 items-center text-xs font-semibold text-muted-foreground">
                                        {workflowCount === 1 ? '1 workflow' : `${workflowCount} workflows`}
                                    </span>
                                </div>

                                <div className="flex items-center gap-x-2">
                                    <div className="flex w-28 justify-start">
                                        {group.configuration && (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Badge
                                                        label={`V${group.configuration.configurationProjectVersion}`}
                                                        styleType="secondary-filled"
                                                        weight="semibold"
                                                    />
                                                </TooltipTrigger>

                                                <TooltipContent>The project version</TooltipContent>
                                            </Tooltip>
                                        )}
                                    </div>

                                    <div className="flex w-72 flex-col items-end gap-y-2">
                                        <div aria-hidden="true" className="min-h-6" />

                                        <Tooltip>
                                            <TooltipTrigger className="flex min-h-6 items-center text-xs text-content-neutral-secondary">
                                                {lastExecutionDate
                                                    ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                                    : 'No executions'}
                                            </TooltipTrigger>

                                            <TooltipContent side="left">Last Execution Date</TooltipContent>
                                        </Tooltip>
                                    </div>

                                    {group.configuration && connectedUser ? (
                                        <ConnectedUserProjectConfigurationMenu
                                            connectedUser={connectedUser}
                                            currentConfiguration={group.configuration}
                                            projectId={group.configuration.projectId}
                                        />
                                    ) : (
                                        <div aria-hidden="true" className="size-9 shrink-0" />
                                    )}
                                </div>
                            </div>

                            <CollapsibleContent className="pl-8">
                                <div className="flex w-full flex-col gap-y-1 pt-3">
                                    <h3 className="flex justify-start pl-3 text-sm heading-tertiary">Workflows</h3>

                                    <ul className="divide-y divide-stroke-neutral-primary">
                                        {renderWorkflowItems(group)}
                                    </ul>
                                </div>
                            </CollapsibleContent>
                        </Collapsible>
                    </section>
                );
            })}
        </div>
    );
};

export default ConnectedUserProjectWorkflowList;
