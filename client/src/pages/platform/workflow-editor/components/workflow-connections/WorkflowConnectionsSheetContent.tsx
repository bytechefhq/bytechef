import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import LoadingDots from '@/components/LoadingDots';
import {Progress} from '@/components/ui/progress';
import {SheetClose} from '@/components/ui/sheet';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PlugIcon, XIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';

import WorkflowConnectionsGroup from './WorkflowConnectionsGroup';
import useSaveWorkflowConnections from './hooks/useSaveWorkflowConnections';
import {
    WorkflowConnectionChangeI,
    WorkflowConnectionGroupI,
    buildWorkflowConnectionGroups,
    filterWorkflowConnectionGroups,
    getWorkflowConnectionsSummary,
} from './utils/workflowConnectionGroups';

export interface WorkflowConnectionsSheetContentProps {
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
    workflowTestConfigurationLoading?: boolean;
}

const WorkflowConnectionsSheetContent = ({
    workflow,
    workflowTestConfiguration,
    workflowTestConfigurationLoading = false,
}: WorkflowConnectionsSheetContentProps) => {
    const [activeGroupId, setActiveGroupId] = useState<string | undefined>();
    const [filterQuery, setFilterQuery] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);

    const {pending, saveWorkflowConnections} = useSaveWorkflowConnections({
        environmentId: currentEnvironmentId,
        workflowId: workflow.id!,
    });

    const groups = useMemo(
        () => buildWorkflowConnectionGroups(workflow, workflowTestConfiguration?.connections),
        [workflow, workflowTestConfiguration?.connections]
    );

    const summary = useMemo(() => getWorkflowConnectionsSummary(groups), [groups]);

    const componentDefinitionsByName = useMemo(
        () =>
            new Map(componentDefinitions.map((componentDefinition) => [componentDefinition.name, componentDefinition])),
        [componentDefinitions]
    );

    const getGroupTitle = useCallback(
        (group: WorkflowConnectionGroupI) => {
            const title = componentDefinitionsByName.get(group.componentName)?.title || group.componentName;

            const componentNameShared = groups.some(
                (otherGroup) => otherGroup.componentName === group.componentName && otherGroup.id !== group.id
            );

            return componentNameShared ? `${title} v${group.componentVersion}` : title;
        },
        [componentDefinitionsByName, groups]
    );

    const filteredGroups = useMemo(
        () => filterWorkflowConnectionGroups(groups, filterQuery, getGroupTitle),
        [filterQuery, getGroupTitle, groups]
    );

    const summaryLabel = `${summary.connectedCount} of ${summary.totalCount} connected`;

    const connectedPercentage =
        summary.totalCount > 0 ? Math.round((summary.connectedCount / summary.totalCount) * 100) : 0;

    const handleChangesRequest = useCallback(
        async (groupId: string, changes: WorkflowConnectionChangeI[]) => {
            if (changes.length === 0) {
                return;
            }

            setActiveGroupId(groupId);

            await saveWorkflowConnections(changes);

            setActiveGroupId((currentGroupId) => (currentGroupId === groupId ? undefined : currentGroupId));
        },
        [saveWorkflowConnections]
    );

    return (
        <>
            <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                <span className="text-lg font-semibold">Workflow Connections</span>

                <div className="flex items-center gap-1">
                    <EnvironmentBadge environmentId={currentEnvironmentId} />

                    <SheetClose asChild>
                        <Button aria-label="Close" disabled={pending} icon={<XIcon />} size="icon" variant="ghost" />
                    </SheetClose>
                </div>
            </header>

            <div className="m-3 flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg bg-surface-neutral-primary">
                {workflowTestConfigurationLoading && (
                    <div aria-busy="true" className="flex flex-1 items-center justify-center p-6">
                        <LoadingDots />
                    </div>
                )}

                {!workflowTestConfigurationLoading && groups.length === 0 && (
                    <div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
                        <PlugIcon aria-hidden="true" className="size-24 text-content-neutral-tertiary" />

                        <h2 className="text-sm font-semibold">No connections needed</h2>

                        <p className="text-sm text-content-neutral-secondary">
                            No task in this workflow uses a connector that needs a connection.
                        </p>
                    </div>
                )}

                {!workflowTestConfigurationLoading && groups.length > 0 && (
                    <>
                        <div className="flex shrink-0 flex-col gap-3 p-3">
                            <div className="flex flex-col gap-2">
                                <div className="flex items-center justify-between gap-3 text-sm">
                                    <span className="font-medium text-content-neutral-primary">{summaryLabel}</span>

                                    {summary.missingCount > 0 && (
                                        <span className="text-content-warning-primary">{`${summary.missingCount} missing`}</span>
                                    )}
                                </div>

                                <Progress
                                    aria-label={summaryLabel}
                                    aria-valuenow={connectedPercentage}
                                    className="h-1.5 bg-surface-brand-secondary *:data-[slot=progress-indicator]:bg-surface-brand-primary"
                                    value={connectedPercentage}
                                />
                            </div>

                            <Input
                                aria-label="Filter by connector or task"
                                onChange={(event) => setFilterQuery(event.target.value)}
                                placeholder="Filter by connector or task"
                                type="search"
                                value={filterQuery}
                            />
                        </div>

                        <div className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto px-3 pb-3">
                            {filteredGroups.length === 0 && (
                                <p className="text-sm text-content-neutral-secondary">
                                    {`No connector or task matches "${filterQuery.trim()}".`}
                                </p>
                            )}

                            {filteredGroups.map((group) => (
                                <WorkflowConnectionsGroup
                                    componentDefinition={componentDefinitionsByName.get(group.componentName)}
                                    disabled={pending}
                                    group={group}
                                    key={group.id}
                                    onChangesRequest={(changes) => handleChangesRequest(group.id, changes)}
                                    pending={pending && activeGroupId === group.id}
                                    title={getGroupTitle(group)}
                                />
                            ))}
                        </div>
                    </>
                )}
            </div>
        </>
    );
};

export default WorkflowConnectionsSheetContent;
