import Button from '@/components/Button/Button';
import {VersionWorkflowChange, WorkflowNodeChangeKind, WorkflowNodeChangeTarget} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useMemo, useState} from 'react';

import WorkflowChangeCanvas from './WorkflowChangeCanvas';
import {collectComponentNames, parseWorkflowDefinition} from './parseWorkflowDefinition';

type SideType = 'new' | 'old';

const LEGEND: {className: string; label: string}[] = [
    {className: 'bg-green-500', label: 'Added'},
    {className: 'bg-amber-500', label: 'Modified'},
    {className: 'bg-blue-500', label: 'Moved'},
    {className: 'bg-red-500', label: 'Removed'},
];

const SIDE_KINDS: Record<SideType, WorkflowNodeChangeKind[]> = {
    new: [WorkflowNodeChangeKind.Added, WorkflowNodeChangeKind.Modified, WorkflowNodeChangeKind.Moved],
    old: [WorkflowNodeChangeKind.Removed, WorkflowNodeChangeKind.Modified, WorkflowNodeChangeKind.Moved],
};

function getRootTaskName(parentPath: string | null | undefined): string | undefined {
    return parentPath ? parentPath.split(/[./]/)[0] : undefined;
}

function deriveHighlights(change: VersionWorkflowChange, side: SideType): Record<string, WorkflowNodeChangeKind> {
    const kinds = SIDE_KINDS[side];
    const highlights: Record<string, WorkflowNodeChangeKind> = {};

    const applicableChanges = change.changes.filter((workflowChange) => kinds.includes(workflowChange.kind));

    applicableChanges.forEach((workflowChange) => {
        if (
            workflowChange.name &&
            (workflowChange.target === WorkflowNodeChangeTarget.Task ||
                workflowChange.target === WorkflowNodeChangeTarget.Trigger)
        ) {
            highlights[workflowChange.name] = workflowChange.kind;
        }
    });

    applicableChanges.forEach((workflowChange) => {
        const rootTaskName = getRootTaskName(workflowChange.parentPath);

        if (
            workflowChange.target === WorkflowNodeChangeTarget.ClusterElement &&
            rootTaskName &&
            !highlights[rootTaskName]
        ) {
            highlights[rootTaskName] = WorkflowNodeChangeKind.Modified;
        }
    });

    return highlights;
}

interface WorkflowChangeCanvasTabProps {
    baseVersionLabel?: string;
    change: VersionWorkflowChange;
    versionLabel: string;
}

const WorkflowChangeCanvasTab = ({baseVersionLabel, change, versionLabel}: WorkflowChangeCanvasTabProps) => {
    const [selectedSide, setSelectedSide] = useState<SideType>('new');

    const hasOldSide = !!change.oldDefinition;
    const hasNewSide = !!change.newDefinition;
    const side: SideType = selectedSide === 'new' && hasNewSide ? 'new' : hasOldSide ? 'old' : 'new';

    const definition = side === 'new' ? change.newDefinition : change.oldDefinition;

    const highlights = useMemo(() => deriveHighlights(change, side), [change, side]);
    const workflow = useMemo(() => {
        if (!definition) {
            return undefined;
        }

        try {
            return parseWorkflowDefinition(definition);
        } catch {
            return undefined;
        }
    }, [definition]);
    const componentNames = useMemo(() => (workflow ? collectComponentNames(workflow) : []), [workflow]);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {actionDefinitions: true, clusterElementDefinitions: true, include: componentNames, triggerDefinitions: true},
        componentNames.length > 0
    );
    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    return (
        <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between gap-4">
                {hasOldSide && hasNewSide ? (
                    <div className="flex gap-1">
                        <Button onClick={() => setSelectedSide('old')} variant={side === 'old' ? 'default' : 'outline'}>
                            {baseVersionLabel}
                        </Button>

                        <Button onClick={() => setSelectedSide('new')} variant={side === 'new' ? 'default' : 'outline'}>
                            {versionLabel}
                        </Button>
                    </div>
                ) : (
                    <span />
                )}

                <ul className="flex items-center gap-3 text-xs text-content-neutral-secondary">
                    {LEGEND.map((legendItem) => (
                        <li className="flex items-center gap-1" key={legendItem.label}>
                            <span className={`size-2.5 rounded-sm ${legendItem.className}`} />

                            {legendItem.label}
                        </li>
                    ))}
                </ul>
            </div>

            <div className="h-[60vh]">
                {workflow && (
                    <WorkflowChangeCanvas
                        componentDefinitions={componentDefinitions ?? []}
                        highlights={highlights}
                        key={side}
                        taskDispatcherDefinitions={taskDispatcherDefinitions ?? []}
                        workflow={workflow}
                    />
                )}
            </div>
        </div>
    );
};

export default WorkflowChangeCanvasTab;
